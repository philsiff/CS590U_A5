import sqlite3

DIST_THRESHOLD_PEOPLE = 0.001

def access_db():
    db_conn = sqlite3.connect("ASSIGNMENT5.db")
    db_c = db_conn.cursor()
    return db_conn, db_c

def release_db(db_conn):
    db_conn.close()

def update_all():
    db_conn, db_c = access_db()

    db_c.execute("SELECT id FROM users")
    response = db_c.fetchall()

    return response

response = [x[0] for x in update_all()]
print(response)