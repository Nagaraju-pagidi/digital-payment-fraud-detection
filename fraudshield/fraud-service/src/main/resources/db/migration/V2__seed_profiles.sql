-- "Normal behaviour" history for the demo customers created by NexaPay's DemoDataSeeder.
-- User ids: 1 Ravi, 2 Priya, 3 Arjun (NEW account, no history), 4 Sneha, 5-10 simulator bots.
-- Demo customers have NO last location here: their "home" and first location are registered
-- at their first login on your laptop (so normal payments from your laptop look normal).

INSERT INTO user_profiles (user_id, approved_count, approved_total, night_count, last_payment_at, last_lat, last_lon, last_city)
VALUES (1, 24, 43200.00, 0, NULL, NULL, NULL, NULL),        -- Ravi:  avg Rs 1,800
       (2, 18, 45000.00, 0, NULL, NULL, NULL, NULL),        -- Priya: avg Rs 2,500
       (4, 30, 180000.00, 2, NULL, NULL, NULL, NULL),       -- Sneha: avg Rs 6,000
       (5, 40, 60000.00, 0, NOW() - INTERVAL '1 day', 22.5726, 88.3639, 'Kolkata'),   -- bots: avg Rs 1,500
       (6, 40, 60000.00, 0, NOW() - INTERVAL '1 day', 26.9124, 75.7873, 'Jaipur'),
       (7, 40, 60000.00, 0, NOW() - INTERVAL '1 day', 13.0827, 80.2707, 'Chennai'),
       (8, 40, 60000.00, 0, NOW() - INTERVAL '1 day', 9.9312, 76.2673, 'Kochi'),
       (9, 40, 60000.00, 0, NOW() - INTERVAL '1 day', 28.6139, 77.2090, 'Delhi'),
       (10, 40, 60000.00, 0, NOW() - INTERVAL '1 day', 26.8467, 80.9462, 'Lucknow');

-- People they usually pay (by UPI ID)
INSERT INTO known_receivers (user_id, receiver_upi)
VALUES (1, 'priya.sharma@fraudpay'), (1, 'sneha.patel@fraudpay'),
       (2, 'ravi.kumar@fraudpay'),   (2, 'sneha.patel@fraudpay'),
       (4, 'ravi.kumar@fraudpay'),   (4, 'priya.sharma@fraudpay');

-- Bots all know each other
INSERT INTO known_receivers (user_id, receiver_upi)
SELECT b.id, r.upi
FROM (VALUES (5), (6), (7), (8), (9), (10)) AS b(id)
CROSS JOIN (VALUES ('amit.bot@fraudpay'), ('divya.bot@fraudpay'), ('karthik.bot@fraudpay'),
                   ('meera.bot@fraudpay'), ('rahul.bot@fraudpay'), ('fatima.bot@fraudpay')) AS r(upi);
