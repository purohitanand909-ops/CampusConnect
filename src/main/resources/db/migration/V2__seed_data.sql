-- =============================================================
-- V2__seed_data.sql
-- Sample data: 1 organizer, 3 students, 4 campus events
-- =============================================================

-- Users -------------------------------------------------------
INSERT INTO users (name, email, role) VALUES
    ('Dr. Priya Sharma',  'priya.sharma@campus.edu',   'ORGANIZER'),
    ('Anand Purohit',     'anand.purohit@student.edu', 'STUDENT'),
    ('Riya Mehta',        'riya.mehta@student.edu',    'STUDENT'),
    ('Karan Verma',       'karan.verma@student.edu',   'STUDENT');

-- Events (organizer_id = 1 = Dr. Priya Sharma) ----------------
-- All events are in the future relative to migration run time,
-- using DATE_ADD so they remain valid regardless of when run.
INSERT INTO events (title, description, category, location, start_time, registration_deadline, capacity, organizer_id) VALUES
(
    'Tech Fest 2026 — Hackathon',
    'A 24-hour hackathon where teams build innovative solutions to real campus problems. Open to all departments. Prizes for top 3 teams.',
    'Technology',
    'Main Auditorium, Block A',
    DATE_ADD(NOW(), INTERVAL 15 DAY),
    DATE_ADD(NOW(), INTERVAL 12 DAY),
    60,
    1
),
(
    'Annual Cultural Night',
    'An evening of music, dance, and drama performances by student clubs. Refreshments provided. Free entry for registered attendees.',
    'Cultural',
    'Open Air Theatre',
    DATE_ADD(NOW(), INTERVAL 20 DAY),
    DATE_ADD(NOW(), INTERVAL 17 DAY),
    200,
    1
),
(
    'Career Fair — Engineering & Tech',
    'Top technology and engineering companies recruiting for internships and full-time positions. Bring printed resumes.',
    'Career',
    'Sports Hall, Block C',
    DATE_ADD(NOW(), INTERVAL 30 DAY),
    DATE_ADD(NOW(), INTERVAL 27 DAY),
    150,
    1
),
(
    'Workshop: Introduction to Machine Learning',
    'Hands-on beginner workshop covering Python basics, NumPy, and your first ML model using scikit-learn. Bring your laptop.',
    'Academic',
    'Computer Lab 201, Block B',
    DATE_ADD(NOW(), INTERVAL 10 DAY),
    DATE_ADD(NOW(), INTERVAL 8 DAY),
    30,
    1
);
