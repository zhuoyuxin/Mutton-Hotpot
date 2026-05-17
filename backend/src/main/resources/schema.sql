CREATE TABLE IF NOT EXISTS merchant_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    must_change_password INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime')),
    update_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS dish_category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    sort_order INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS dish (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER,
    name TEXT NOT NULL,
    price INTEGER NOT NULL,
    allow_half_portion INTEGER DEFAULT 0,
    half_price INTEGER,
    image TEXT,
    description TEXT,
    status INTEGER DEFAULT 1,
    stock INTEGER DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime')),
    update_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS table_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    area TEXT DEFAULT '大厅',
    status INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS dining_session (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    table_id INTEGER,
    status INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime')),
    end_time DATETIME
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_active_session_per_table
    ON dining_session(table_id) WHERE status = 0 AND table_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS session_checkout (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER UNIQUE NOT NULL,
    total_amount INTEGER DEFAULT 0,
    actual_paid INTEGER DEFAULT 0,
    discount_amount INTEGER DEFAULT 0,
    customer_id INTEGER,
    points_earned INTEGER DEFAULT 0,
    checkout_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS customer (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    phone TEXT,
    openid TEXT,
    name TEXT,
    points INTEGER DEFAULT 0,
    total_spent INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime')),
    update_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no TEXT UNIQUE NOT NULL,
    session_id INTEGER NOT NULL,
    table_id INTEGER,
    customer_id INTEGER,
    total_amount INTEGER DEFAULT 0,
    status INTEGER DEFAULT 0,
    remark TEXT,
    create_time DATETIME DEFAULT (datetime('now','localtime')),
    update_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS order_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    dish_id INTEGER NOT NULL,
    dish_name TEXT NOT NULL,
    dish_price INTEGER NOT NULL,
    portion_type TEXT NOT NULL DEFAULT 'FULL',
    quantity INTEGER NOT NULL,
    status INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS points_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    checkout_id INTEGER,
    points INTEGER NOT NULL,
    type INTEGER DEFAULT 0,
    remark TEXT,
    create_time DATETIME DEFAULT (datetime('now','localtime'))
);
