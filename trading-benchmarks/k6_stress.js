import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 500,
  duration: '30s',
};

export default function () {
  const url = 'http://localhost:8080/api/orders';
  
  // Randomly distribute order parameters to mimic realistic concurrent market trading
  const traderId = Math.floor(Math.random() * 100) + 1000;
  const side = Math.random() < 0.5 ? 'BUY' : 'SELL';
  const price = Math.floor(Math.random() * 1000) + 10000;
  const quantity = Math.floor(Math.random() * 100) + 1;

  const payload = JSON.stringify({
    traderId: traderId,
    side: side,
    type: 'LIMIT',
    price: price,
    quantity: quantity
  });

  const params = {
    headers: {
      'Content-Type': 'application/json'
    }
  };

  const res = http.post(url, payload, params);
  
  check(res, {
    'status is 200': (r) => r.status === 200
  });
}
