import { useState, useEffect, useRef, useCallback } from 'react';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

const getWebSocketUrl = () => {
  const configuredUrl = import.meta.env.VITE_WS_URL;
  if (configuredUrl) return configuredUrl;

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws/market-data`;
};

export function useMarketData() {
  const [isConnected, setIsConnected] = useState(false);
  const [orderBook, setOrderBook] = useState({ bids: [], asks: [] });
  const [trades, setTrades] = useState([]);
  const [metrics, setMetrics] = useState({
    totalOrdersProcessed: 0,
    minTotalLatencyNs: 0,
    maxTotalLatencyNs: 0,
    avgTotalLatencyNs: 0,
    p50TotalLatencyNs: 0,
    p90TotalLatencyNs: 0,
    p99TotalLatencyNs: 0,
  });
  
  // Local tracking of orders submitted by the client
  const [myOrders, setMyOrders] = useState([]);
  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);

  // Poll metrics from backend
  const fetchMetrics = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE}/metrics`);
      if (response.ok) {
        const data = await response.json();
        setMetrics(data);
      }
    } catch (err) {
      console.warn('Metrics polling error:', err.message);
    }
  }, []);

  // Fetch initial order book snapshot
  const fetchOrderBook = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE}/book`);
      if (response.ok) {
        const data = await response.json();
        setOrderBook({
          bids: data.bids || [],
          asks: data.asks || [],
        });
      }
    } catch (err) {
      console.warn('Order book snapshot error:', err.message);
    }
  }, []);

  // WebSocket Connection
  const connectWS = useCallback(() => {
    if (wsRef.current) return;

    const ws = new WebSocket(getWebSocketUrl());
    wsRef.current = ws;

    ws.onopen = () => {
      setIsConnected(true);
      console.log('Market data WebSocket connected.');
      fetchOrderBook();
      fetchMetrics();
    };

    ws.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data);
        if (payload.event === 'trades' && Array.isArray(payload.data)) {
          const newTrades = payload.data.map(t => ({
            ...t,
            receivedAt: Date.now(),
            side: t.buyOrderId > t.sellOrderId ? 'BUY' : 'SELL', // heuristic for initiator
          }));
          
          setTrades((prev) => [
            ...newTrades.reverse(),
            ...prev,
          ].slice(0, 40));

          // Also trigger a metrics refresh on new executions
          fetchMetrics();
        } else if (payload.event === 'book_update') {
          setOrderBook({
            bids: payload.data.bids || [],
            asks: payload.data.asks || [],
          });
        }
      } catch (err) {
        console.error('Failed parsing WebSocket message:', err);
      }
    };

    ws.onclose = () => {
      setIsConnected(false);
      wsRef.current = null;
      console.log('WebSocket closed. Retrying connection in 3 seconds...');
      reconnectTimeoutRef.current = setTimeout(connectWS, 3000);
    };

    ws.onerror = (err) => {
      console.error('WebSocket error:', err);
      ws.close();
    };
  }, [fetchOrderBook, fetchMetrics]);

  useEffect(() => {
    connectWS();
    const interval = setInterval(fetchMetrics, 3000);
    return () => {
      clearInterval(interval);
      if (wsRef.current) wsRef.current.close();
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);
    };
  }, [connectWS, fetchMetrics]);

  // Submit Order API call
  const submitOrder = async (orderParams) => {
    try {
      const response = await fetch(`${API_BASE}/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(orderParams),
      });

      if (!response.ok) throw new Error('Order submission failed');
      const data = await response.json();
      
      // Calculate filled quantity from returning trades
      const fillQty = data.trades ? data.trades.reduce((sum, t) => sum + t.quantity, 0) : 0;
      const remaining = Math.max(0, orderParams.quantity - fillQty);
      
      let status = 'ACTIVE';
      if (remaining === 0) status = 'FILLED';
      else if (fillQty > 0) status = 'PARTIALLY_FILLED';

      const newOrder = {
        orderId: data.order.orderId,
        traderId: orderParams.traderId,
        symbol: orderParams.symbol,
        side: orderParams.side,
        type: orderParams.type,
        price: orderParams.price,
        quantity: orderParams.quantity,
        remainingQuantity: remaining,
        status: status,
        timestamp: data.order.timestamp,
      };

      if (status !== 'FILLED') {
        setMyOrders(prev => [newOrder, ...prev]);
      }
      
      fetchOrderBook();
      fetchMetrics();
      return { success: true, ...data };
    } catch (err) {
      console.error(err);
      return { success: false, error: err.message };
    }
  };

  // Cancel Order API call
  const cancelOrder = async (orderId) => {
    try {
      const response = await fetch(`${API_BASE}/orders/${orderId}`, {
        method: 'DELETE',
      });

      if (!response.ok) throw new Error('Cancel request failed');
      const success = await response.json();
      if (success) {
        setMyOrders(prev => prev.map(order => 
          order.orderId === orderId ? { ...order, status: 'CANCELED', remainingQuantity: 0 } : order
        ).filter(order => order.status !== 'CANCELED')); // remove canceled from active list
      }
      fetchOrderBook();
      fetchMetrics();
      return success;
    } catch (err) {
      console.error(err);
      return false;
    }
  };

  // Clear Engine State
  const clearEngine = async () => {
    try {
      const response = await fetch(`${API_BASE}/clear`, { method: 'POST' });
      if (response.ok) {
        setTrades([]);
        setMyOrders([]);
        setOrderBook({ bids: [], asks: [] });
        fetchMetrics();
        return true;
      }
    } catch (err) {
      console.error(err);
    }
    return false;
  };

  // Refresh active orders local quantities by checking trades
  useEffect(() => {
    if (trades.length === 0 || myOrders.length === 0) return;

    setMyOrders(prevOrders => {
      let changed = false;
      const updated = prevOrders.map(order => {
        // Find if this order matches any recent trades
        const orderTrades = trades.filter(t => 
          (order.side === 'BUY' && t.buyOrderId === order.orderId) ||
          (order.side === 'SELL' && t.sellOrderId === order.orderId)
        );

        if (orderTrades.length === 0) return order;

        const totalTraded = orderTrades.reduce((sum, t) => sum + t.quantity, 0);
        const rem = Math.max(0, order.quantity - totalTraded);
        const status = rem === 0 ? 'FILLED' : 'PARTIALLY_FILLED';

        if (order.remainingQuantity !== rem || order.status !== status) {
          changed = true;
          return { ...order, remainingQuantity: rem, status };
        }
        return order;
      }).filter(order => order.status !== 'FILLED'); // sweep out filled orders

      return changed ? updated : prevOrders;
    });
  }, [trades, myOrders]);

  return {
    isConnected,
    orderBook,
    trades,
    metrics,
    myOrders,
    submitOrder,
    cancelOrder,
    clearEngine,
    refreshMetrics: fetchMetrics,
    refreshOrderBook: fetchOrderBook
  };
}
