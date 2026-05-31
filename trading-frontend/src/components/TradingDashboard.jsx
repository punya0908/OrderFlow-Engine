import React, { useState } from 'react';
import { useMarketData } from '../hooks/useMarketData';
import { 
  TrendingUp, 
  Activity, 
  Trash2, 
  Zap, 
  Layers, 
  RefreshCw, 
  ArrowUpRight, 
  ArrowDownRight, 
  AlertCircle 
} from 'lucide-react';

export default function TradingDashboard() {
  const {
    isConnected,
    orderBook,
    trades,
    metrics,
    myOrders,
    submitOrder,
    cancelOrder,
    clearEngine
  } = useMarketData();

  // Form states
  const [traderId, setTraderId] = useState('1001');
  const [symbol, setSymbol] = useState('AAPL');
  const [side, setSide] = useState('BUY');
  const [orderType, setOrderType] = useState('LIMIT');
  const [price, setPrice] = useState('150.00');
  const [quantity, setQuantity] = useState('100');
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState(null);

  // Formatting helpers
  const formatPrice = (p) => (p / 100.0).toFixed(2);
  const formatTime = (ts) => {
    const d = new Date(ts);
    return d.toTimeString().split(' ')[0];
  };

  const formatLatency = (ns) => {
    if (ns === undefined || ns === null) return '0 ns';
    if (ns < 1000) return `${ns} ns`;
    if (ns < 1000000) return `${(ns / 1000.0).toFixed(2)} µs`;
    return `${(ns / 1000000.0).toFixed(2)} ms`;
  };

  // Submit Handler
  const handleOrderSubmit = async (e) => {
    e.preventDefault();
    if (loading) return;

    const parsedPrice = Math.round(parseFloat(price) * 100);
    const parsedQty = parseInt(quantity);
    const parsedTrader = parseInt(traderId);

    if (isNaN(parsedQty) || parsedQty <= 0) {
      showFeedback('Invalid quantity', 'error');
      return;
    }
    if (orderType === 'LIMIT' && (isNaN(parsedPrice) || parsedPrice <= 0)) {
      showFeedback('Invalid price', 'error');
      return;
    }

    setLoading(true);
    const result = await submitOrder({
      traderId: parsedTrader,
      symbol,
      side,
      type: orderType,
      price: parsedPrice,
      quantity: parsedQty
    });

    setLoading(false);
    if (result.success) {
      const matchText = result.trades && result.trades.length > 0 
        ? `Matched ${result.trades.reduce((s, t) => s + t.quantity, 0)} shares!` 
        : 'Order placed in book.';
      showFeedback(`Order #${result.order.orderId} submitted. ${matchText}`, 'success');
    } else {
      showFeedback(`Error: ${result.error}`, 'error');
    }
  };

  const showFeedback = (msg, type) => {
    setFeedback({ msg, type });
    setTimeout(() => setFeedback(null), 4000);
  };

  // Aggregated depth helpers
  const maxBidTotal = orderBook.bids.reduce((sum, b) => sum + b.quantity, 0) || 1;
  const maxAskTotal = orderBook.asks.reduce((sum, a) => sum + a.quantity, 0) || 1;
  const maxCumulative = Math.max(maxBidTotal, maxAskTotal);

  // Compute best bid/ask and spread
  const bestBid = orderBook.bids.length > 0 ? orderBook.bids[0].price : null;
  const bestAsk = orderBook.asks.length > 0 ? orderBook.asks[0].price : null;
  const spread = bestBid && bestAsk ? bestAsk - bestBid : null;

  return (
    <div style={{ padding: '1.5rem', maxWidth: '1600px', margin: '0 auto' }}>
      
      {/* Top Banner & Title */}
      <header style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: '1.5rem',
        flexWrap: 'wrap',
        gap: '1rem'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Zap size={32} color="var(--color-accent)" style={{ filter: 'drop-shadow(0 0 8px var(--color-accent-glow))' }} />
          <div>
            <h1 style={{ fontSize: '1.8rem', fontWeight: '800', letterSpacing: '-0.5px' }}>
              ApexEngine
            </h1>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
              In-Memory FIFO Matching Engine
            </span>
          </div>
        </div>

        {/* Status bar & Reset */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div className={`pulse-badge ${isConnected ? '' : 'disconnected'}`}>
            <span className="pulse-dot"></span>
            {isConnected ? 'LIVE ENGINE CONNECTED' : 'ENGINE DISCONNECTED'}
          </div>

          <button 
            onClick={clearEngine}
            style={{
              background: 'rgba(255, 60, 60, 0.1)',
              border: '1px solid rgba(255, 60, 60, 0.2)',
              color: '#ff6b6b',
              padding: '0.5rem 1rem',
              borderRadius: '8px',
              fontWeight: '600',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              transition: 'all 0.2s ease',
              fontFamily: 'var(--font-sans)',
              fontSize: '0.85rem'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = 'rgba(255, 60, 60, 0.2)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'rgba(255, 60, 60, 0.1)';
            }}
          >
            <Trash2 size={16} />
            Reset Simulator
          </button>
        </div>
      </header>

      {/* Grid Layout */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: '320px 1fr 340px', 
        gap: '1.5rem',
        alignItems: 'start',
        marginBottom: '1.5rem'
      }}>
        
        {/* PANEL 1: ORDER ENTRY */}
        <section className="panel" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', borderBottom: '1px solid var(--panel-border)', paddingBottom: '0.75rem' }}>
            <TrendingUp size={20} color="var(--color-accent)" />
            <h2 style={{ fontSize: '1.1rem', fontWeight: '700' }}>New Order</h2>
          </div>

          <form onSubmit={handleOrderSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            
            {/* Symbol Selection */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
              <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: '600' }}>SYMBOL</label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.35rem' }}>
                {['AAPL', 'MSFT', 'TSLA', 'NVDA'].map(sym => (
                  <button
                    key={sym}
                    type="button"
                    onClick={() => setSymbol(sym)}
                    style={{
                      padding: '0.5rem',
                      borderRadius: '6px',
                      border: symbol === sym ? '1px solid var(--color-accent)' : '1px solid var(--panel-border)',
                      background: symbol === sym ? 'var(--color-accent-glow)' : 'transparent',
                      color: symbol === sym ? 'var(--color-accent)' : 'var(--text-primary)',
                      fontWeight: '700',
                      cursor: 'pointer',
                      fontSize: '0.8rem',
                      fontFamily: 'var(--font-sans)',
                      transition: 'all 0.2s ease'
                    }}
                  >
                    {sym}
                  </button>
                ))}
              </div>
            </div>

            {/* BUY / SELL Toggle */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', padding: '0.2rem', background: 'rgba(0,0,0,0.2)', borderRadius: '8px' }}>
              <button
                type="button"
                onClick={() => setSide('BUY')}
                style={{
                  padding: '0.5rem',
                  borderRadius: '6px',
                  border: 'none',
                  background: side === 'BUY' ? 'var(--color-buy)' : 'transparent',
                  color: side === 'BUY' ? '#fff' : 'var(--text-secondary)',
                  fontWeight: '700',
                  cursor: 'pointer',
                  fontSize: '0.85rem',
                  fontFamily: 'var(--font-sans)',
                  transition: 'all 0.2s'
                }}
              >
                BUY
              </button>
              <button
                type="button"
                onClick={() => setSide('SELL')}
                style={{
                  padding: '0.5rem',
                  borderRadius: '6px',
                  border: 'none',
                  background: side === 'SELL' ? 'var(--color-sell)' : 'transparent',
                  color: side === 'SELL' ? '#fff' : 'var(--text-secondary)',
                  fontWeight: '700',
                  cursor: 'pointer',
                  fontSize: '0.85rem',
                  fontFamily: 'var(--font-sans)',
                  transition: 'all 0.2s'
                }}
              >
                SELL
              </button>
            </div>

            {/* LIMIT / MARKET Toggle */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
              <button
                type="button"
                onClick={() => setOrderType('LIMIT')}
                style={{
                  padding: '0.4rem',
                  borderRadius: '6px',
                  border: '1px solid var(--panel-border)',
                  background: orderType === 'LIMIT' ? 'rgba(255,255,255,0.06)' : 'transparent',
                  color: orderType === 'LIMIT' ? 'var(--text-primary)' : 'var(--text-secondary)',
                  fontWeight: '600',
                  cursor: 'pointer',
                  fontSize: '0.8rem',
                  fontFamily: 'var(--font-sans)',
                  transition: 'all 0.2s'
                }}
              >
                LIMIT
              </button>
              <button
                type="button"
                onClick={() => setOrderType('MARKET')}
                style={{
                  padding: '0.4rem',
                  borderRadius: '6px',
                  border: '1px solid var(--panel-border)',
                  background: orderType === 'MARKET' ? 'rgba(255,255,255,0.06)' : 'transparent',
                  color: orderType === 'MARKET' ? 'var(--text-primary)' : 'var(--text-secondary)',
                  fontWeight: '600',
                  cursor: 'pointer',
                  fontSize: '0.8rem',
                  fontFamily: 'var(--font-sans)',
                  transition: 'all 0.2s'
                }}
              >
                MARKET
              </button>
            </div>

            {/* Inputs */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              
              {/* Price Field */}
              {orderType === 'LIMIT' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: '600' }}>PRICE (USD)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={price}
                    onChange={(e) => setPrice(e.target.value)}
                    style={{
                      background: 'rgba(0,0,0,0.25)',
                      border: '1px solid var(--panel-border)',
                      borderRadius: '6px',
                      padding: '0.5rem 0.75rem',
                      color: 'var(--text-primary)',
                      fontSize: '0.95rem',
                      fontWeight: '600',
                      fontFamily: 'var(--font-sans)',
                      width: '100%'
                    }}
                  />
                </div>
              )}

              {/* Quantity Field */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: '600' }}>QUANTITY</label>
                <input
                  type="number"
                  min="1"
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  style={{
                    background: 'rgba(0,0,0,0.25)',
                    border: '1px solid var(--panel-border)',
                    borderRadius: '6px',
                    padding: '0.5rem 0.75rem',
                    color: 'var(--text-primary)',
                    fontSize: '0.95rem',
                    fontWeight: '600',
                    fontFamily: 'var(--font-sans)',
                    width: '100%'
                  }}
                />
              </div>

              {/* Trader ID Field */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: '600' }}>TRADER ID</label>
                <input
                  type="number"
                  value={traderId}
                  onChange={(e) => setTraderId(e.target.value)}
                  style={{
                    background: 'rgba(0,0,0,0.25)',
                    border: '1px solid var(--panel-border)',
                    borderRadius: '6px',
                    padding: '0.5rem 0.75rem',
                    color: 'var(--text-primary)',
                    fontSize: '0.95rem',
                    fontWeight: '600',
                    fontFamily: 'var(--font-sans)',
                    width: '100%'
                  }}
                />
              </div>

            </div>

            {/* Submission Feedback */}
            {feedback && (
              <div style={{
                padding: '0.5rem 0.75rem',
                borderRadius: '6px',
                fontSize: '0.8rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                background: feedback.type === 'success' ? 'rgba(2, 192, 118, 0.1)' : 'rgba(246, 70, 93, 0.1)',
                border: feedback.type === 'success' ? '1px solid rgba(2, 192, 118, 0.2)' : '1px solid rgba(246, 70, 93, 0.2)',
                color: feedback.type === 'success' ? 'var(--color-buy)' : 'var(--color-sell)',
              }}>
                <AlertCircle size={14} />
                <span>{feedback.msg}</span>
              </div>
            )}

            {/* Action Button */}
            <button 
              type="submit" 
              className={side === 'BUY' ? 'btn-buy' : 'btn-sell'}
              disabled={loading}
              style={{ opacity: loading ? 0.7 : 1 }}
            >
              {loading ? 'PROCESSING...' : `${side} ${symbol}`}
            </button>

          </form>
        </section>

        {/* PANEL 2: ORDER BOOK */}
        <section className="panel" style={{ minHeight: '480px', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--panel-border)', paddingBottom: '0.75rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Layers size={20} color="var(--color-accent)" />
              <h2 style={{ fontSize: '1.1rem', fontWeight: '700' }}>Order Book Depth ({symbol})</h2>
            </div>
            {spread && (
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: '600' }}>
                SPREAD: <span style={{ color: 'var(--text-primary)' }}>${(spread / 100.0).toFixed(2)}</span>
              </span>
            )}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', height: '100%', justifyContent: 'space-between' }}>
            
            {/* Asks (Sells) - Sorted Descending (highest on top, best ask at bottom of asks) */}
            <div style={{ display: 'flex', flexDirection: 'column-reverse', gap: '1px' }}>
              {orderBook.asks.slice(0, 10).map((ask, index) => {
                const percent = Math.min(100, (ask.quantity / maxCumulative) * 100);
                return (
                  <div key={`ask-${index}`} className="order-row">
                    <span className="row-val" style={{ color: 'var(--color-sell)' }}>${formatPrice(ask.price)}</span>
                    <span className="row-val" style={{ textAlign: 'right' }}>{ask.quantity}</span>
                    <span className="row-val" style={{ textAlign: 'right', color: 'var(--text-secondary)' }}>
                      {(orderBook.asks.slice(0, index + 1).reduce((s, a) => s + a.quantity, 0))}
                    </span>
                    <div className="depth-bar sell" style={{ width: `${percent}%` }}></div>
                  </div>
                );
              })}
              {orderBook.asks.length === 0 && (
                <div style={{ padding: '1rem', textAlign: 'center', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                  No Asks
                </div>
              )}
            </div>

            {/* Spread Indicator Line */}
            <div style={{ 
              padding: '0.5rem 0.5rem', 
              background: 'rgba(255,255,255,0.02)', 
              borderTop: '1px solid var(--panel-border)', 
              borderBottom: '1px solid var(--panel-border)',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              fontSize: '0.9rem',
              fontWeight: '700'
            }}>
              <span style={{ color: bestBid ? 'var(--color-buy)' : 'var(--text-secondary)' }}>
                Bid: {bestBid ? `$${formatPrice(bestBid)}` : '-'}
              </span>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
                Mid: {bestBid && bestAsk ? `$${formatPrice((bestBid + bestAsk) / 2)}` : '-'}
              </span>
              <span style={{ color: bestAsk ? 'var(--color-sell)' : 'var(--text-secondary)' }}>
                Ask: {bestAsk ? `$${formatPrice(bestAsk)}` : '-'}
              </span>
            </div>

            {/* Bids (Buys) - Sorted Descending (best bid at top of bids) */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1px' }}>
              {orderBook.bids.slice(0, 10).map((bid, index) => {
                const percent = Math.min(100, (bid.quantity / maxCumulative) * 100);
                return (
                  <div key={`bid-${index}`} className="order-row">
                    <span className="row-val" style={{ color: 'var(--color-buy)' }}>${formatPrice(bid.price)}</span>
                    <span className="row-val" style={{ textAlign: 'right' }}>{bid.quantity}</span>
                    <span className="row-val" style={{ textAlign: 'right', color: 'var(--text-secondary)' }}>
                      {(orderBook.bids.slice(0, index + 1).reduce((s, b) => s + b.quantity, 0))}
                    </span>
                    <div className="depth-bar buy" style={{ width: `${percent}%` }}></div>
                  </div>
                );
              })}
              {orderBook.bids.length === 0 && (
                <div style={{ padding: '1rem', textAlign: 'center', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                  No Bids
                </div>
              )}
            </div>

          </div>
        </section>

        {/* PANEL 3: REAL-TIME TRADES FEED */}
        <section className="panel" style={{ minHeight: '480px', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', borderBottom: '1px solid var(--panel-border)', paddingBottom: '0.75rem' }}>
            <Activity size={20} color="var(--color-accent)" />
            <h2 style={{ fontSize: '1.1rem', fontWeight: '700' }}>Live Trades</h2>
          </div>

          <div style={{ 
            display: 'flex', 
            flexDirection: 'column', 
            gap: '4px',
            overflowY: 'auto',
            maxHeight: '420px',
            paddingRight: '0.2rem'
          }}>
            {trades.map((t, idx) => {
              const isNew = (Date.now() - t.receivedAt) < 600;
              const flashClass = isNew ? (t.side === 'BUY' ? 'new-buy' : 'new-sell') : '';
              return (
                <div 
                  key={`${t.tradeId}-${idx}`} 
                  className={`trade-row ${flashClass} animate-slide`}
                  style={{ borderLeft: `3px solid ${t.side === 'BUY' ? 'var(--color-buy)' : 'var(--color-sell)'}` }}
                >
                  <span style={{ fontWeight: '700', width: '50px' }}>{t.symbol || 'AAPL'}</span>
                  <span style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>{formatTime(t.timestamp)}</span>
                  <span style={{ color: t.side === 'BUY' ? 'var(--color-buy)' : 'var(--color-sell)', fontWeight: '600' }}>
                    ${formatPrice(t.price)}
                  </span>
                  <span style={{ fontWeight: '600' }}>{t.quantity} qty</span>
                </div>
              );
            })}
            {trades.length === 0 && (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                Waiting for matches...
              </div>
            )}
          </div>
        </section>

      </div>

      {/* Latency Metrics Dashboard & My Orders */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: '1fr 1.5fr', 
        gap: '1.5rem',
        alignItems: 'start'
      }}>
        
        {/* Latency Dashboard */}
        <section className="panel" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', borderBottom: '1px solid var(--panel-border)', paddingBottom: '0.75rem' }}>
            <Zap size={20} color="var(--color-accent)" />
            <h2 style={{ fontSize: '1.1rem', fontWeight: '700' }}>Engine Latency Dashboard</h2>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            
            <div style={{ background: 'rgba(0,0,0,0.2)', padding: '0.75rem', borderRadius: '8px', border: '1px solid var(--panel-border)' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: '600' }}>AVERAGE LATENCY</span>
              <p style={{ fontSize: '1.3rem', fontWeight: '800', color: 'var(--color-accent)', marginTop: '0.2rem' }}>
                {formatLatency(metrics.avgTotalLatencyNs)}
              </p>
            </div>

            <div style={{ background: 'rgba(0,0,0,0.2)', padding: '0.75rem', borderRadius: '8px', border: '1px solid var(--panel-border)' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: '600' }}>P50 LATENCY</span>
              <p style={{ fontSize: '1.3rem', fontWeight: '800', color: '#a78bfa', marginTop: '0.2rem' }}>
                {formatLatency(metrics.p50TotalLatencyNs)}
              </p>
            </div>

            <div style={{ background: 'rgba(0,0,0,0.2)', padding: '0.75rem', borderRadius: '8px', border: '1px solid var(--panel-border)' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: '600' }}>P90 LATENCY</span>
              <p style={{ fontSize: '1.3rem', fontWeight: '800', color: '#f472b6', marginTop: '0.2rem' }}>
                {formatLatency(metrics.p90TotalLatencyNs)}
              </p>
            </div>

            <div style={{ background: 'rgba(0,0,0,0.2)', padding: '0.75rem', borderRadius: '8px', border: '1px solid var(--panel-border)' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: '600' }}>P99 LATENCY</span>
              <p style={{ fontSize: '1.3rem', fontWeight: '800', color: '#f87171', marginTop: '0.2rem' }}>
                {formatLatency(metrics.p99TotalLatencyNs)}
              </p>
            </div>

          </div>

          <div style={{ 
            marginTop: '0.5rem', 
            background: 'var(--bg-color)', 
            padding: '0.5rem 0.75rem', 
            borderRadius: '6px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            fontSize: '0.85rem'
          }}>
            <span style={{ color: 'var(--text-secondary)' }}>Total Orders Processed:</span>
            <span style={{ fontWeight: '700', color: 'var(--color-accent)' }}>
              {metrics.totalOrdersProcessed.toLocaleString()}
            </span>
          </div>
        </section>

        {/* My Active Orders List */}
        <section className="panel" style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minHeight: '235px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', borderBottom: '1px solid var(--panel-border)', paddingBottom: '0.75rem' }}>
            <Layers size={20} color="var(--color-accent)" />
            <h2 style={{ fontSize: '1.1rem', fontWeight: '700' }}>My Active Resting Orders</h2>
          </div>

          <div style={{ overflowX: 'auto', width: '100%' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--panel-border)', color: 'var(--text-secondary)' }}>
                  <th style={{ padding: '0.5rem' }}>ORDER ID</th>
                  <th style={{ padding: '0.5rem' }}>SYMBOL</th>
                  <th style={{ padding: '0.5rem' }}>SIDE</th>
                  <th style={{ padding: '0.5rem' }}>PRICE</th>
                  <th style={{ padding: '0.5rem' }}>QTY (REMAIN)</th>
                  <th style={{ padding: '0.5rem' }}>STATUS</th>
                  <th style={{ padding: '0.5rem', textAlign: 'right' }}>ACTION</th>
                </tr>
              </thead>
              <tbody>
                {myOrders.map(order => (
                  <tr key={order.orderId} style={{ borderBottom: '1px solid rgba(255,255,255,0.02)' }}>
                    <td style={{ padding: '0.5rem', fontWeight: '600' }}>#{order.orderId}</td>
                    <td style={{ padding: '0.5rem', fontWeight: '700' }}>{order.symbol}</td>
                    <td style={{ 
                      padding: '0.5rem', 
                      fontWeight: '700',
                      color: order.side === 'BUY' ? 'var(--color-buy)' : 'var(--color-sell)'
                    }}>
                      {order.side}
                    </td>
                    <td style={{ padding: '0.5rem', fontWeight: '600' }}>
                      {order.type === 'MARKET' ? 'MARKET' : `$${formatPrice(order.price)}`}
                    </td>
                    <td style={{ padding: '0.5rem' }}>{order.remainingQuantity} / {order.quantity}</td>
                    <td style={{ padding: '0.5rem' }}>
                      <span style={{ 
                        padding: '0.15rem 0.35rem', 
                        borderRadius: '4px', 
                        fontSize: '0.75rem',
                        fontWeight: '600',
                        background: order.status === 'PARTIALLY_FILLED' ? 'rgba(255, 193, 7, 0.1)' : 'rgba(0, 240, 255, 0.1)',
                        color: order.status === 'PARTIALLY_FILLED' ? '#ffc107' : 'var(--color-accent)',
                      }}>
                        {order.status}
                      </span>
                    </td>
                    <td style={{ padding: '0.5rem', textAlign: 'right' }}>
                      <button
                        onClick={() => cancelOrder(order.orderId)}
                        style={{
                          background: 'transparent',
                          border: 'none',
                          color: '#ff6b6b',
                          cursor: 'pointer',
                          padding: '0.2rem',
                          borderRadius: '4px',
                          transition: 'background-color 0.2s'
                        }}
                        onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'rgba(255, 107, 107, 0.1)'}
                        onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                      >
                        Cancel
                      </button>
                    </td>
                  </tr>
                ))}
                {myOrders.length === 0 && (
                  <tr>
                    <td colSpan="7" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                      No active orders placed from this session.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

      </div>

    </div>
  );
}
