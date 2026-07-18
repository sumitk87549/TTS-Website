INSERT INTO voice (engine_voice_id, display_name, gender, style_tag) VALUES
('M1', 'Rohan',  'male',   'Calm'),
('M2', 'Aryan',  'male',   'Dynamic'),
('M3', 'Kabir',  'male',   'Steady'),
('M4', 'Dev',    'male',   'Warm'),
('M5', 'Vihaan', 'male',   'High Energy'),
('F1', 'Isha',   'female', 'Warm'),
('F2', 'Meera',  'female', 'Calm'),
('F3', 'Priya',  'female', 'Dynamic'),
('F4', 'Kavya',  'female', 'High Energy'),
('F5', 'Naina',  'female', 'Steady')
ON CONFLICT (engine_voice_id) DO NOTHING;
