import socket
import threading
import datetime
import json
import sqlite3
from sklearn.cluster import KMeans
import numpy as np

def access_db():
    db_lock.acquire()
    db_conn = sqlite3.connect("ASSIGNMENT5.db")
    db_c = db_conn.cursor()
    return db_conn, db_c

def release_db(db_conn):
    db_conn.close()
    db_lock.release()

def num_people_near(msg_id, time_diff):
    db_conn, db_c = access_db()
    db_c.execute("SELECT time, lat, lon FROM data WHERE id = ? AND time >= (SELECT DATETIME('now', '-4 hour','-" + str(time_diff) + " day'))", str(msg_id))
    coords = db_c.fetchall()
    release_db(db_conn)

    ids = set()
    db_conn, db_c = access_db()
    for i in range(len(coords)):
        query = "SELECT id FROM data WHERE id != " + str(msg_id) + " AND (time >= (SELECT DATETIME('" + str(coords[i][0]) + "', '-2.5 minute')) AND time <= (SELECT DATETIME('"+str(coords[i][0])+"', '+2.5 minute'))) AND (lat >= "+str(coords[i][1])+" - "+str(DIST_THRESHOLD_PEOPLE)+" AND lat <= "+str(coords[i][1])+" + "+str(DIST_THRESHOLD_PEOPLE)+") AND (lon >= "+str(coords[i][2])+" - "+str(DIST_THRESHOLD_PEOPLE)+" AND lon <= "+str(coords[i][2])+" + "+str(DIST_THRESHOLD_PEOPLE)+")"
        db_c.execute(query)
        ids = ids.union(set(db_c.fetchall()))
    release_db(db_conn)
    return len(ids)

def times_left_home(msg_id, time_diff):
    db_conn, db_c = access_db()

    db_c.execute("SELECT home_lat, home_lon FROM users WHERE id = ?", (str(msg_id)))
    home_lat, home_lon = db_c.fetchone();
    lat_min = home_lat - DIST_THRESHOLD_HOME
    lat_max = home_lat + DIST_THRESHOLD_HOME
    lon_min = home_lon - DIST_THRESHOLD_HOME
    lon_max = home_lon + DIST_THRESHOLD_HOME

    query = "SELECT lat, lon FROM data WHERE id = " + str(msg_id) + " AND (lat > "+str(lat_max)+" OR lat < "+str(lat_min)+" OR lon > "+str(lon_max)+" OR lon < "+str(lon_min)+") AND time >= (SELECT DATETIME('now','-4 hour','-" + str(time_diff) + " day'))"
    db_c.execute(query)
    coords = db_c.fetchall()

    release_db(db_conn)

    return len(coords)

def get_trips(msg_id, time_diff):
    db_conn, db_c = access_db()

    db_c.execute("SELECT home_lat, home_lon FROM users WHERE id = ?", (str(msg_id)))
    home_lat, home_lon = db_c.fetchone();
    lat_min = home_lat - DIST_THRESHOLD_HOME
    lat_max = home_lat + DIST_THRESHOLD_HOME
    lon_min = home_lon - DIST_THRESHOLD_HOME
    lon_max = home_lon + DIST_THRESHOLD_HOME

    query = "SELECT time, lat, lon FROM data WHERE id = " + str(msg_id) + " AND (lat > "+str(lat_max)+" OR lat < "+str(lat_min)+" OR lon > "+str(lon_max)+" OR lon < "+str(lon_min)+") AND time >= (SELECT DATETIME('now','-4 hour','-" + str(time_diff) + " day'))"
    db_c.execute(query)
    coords = db_c.fetchall()

    release_db(db_conn)

    return coords

def set_home(msg_id):
    db_conn, db_c = access_db()
    db_c.execute("SELECT lat, lon FROM data WHERE id = ?", (str(msg_id)))
    coords = db_c.fetchall()
    release_db(db_conn)

    X = np.array(coords)
    kmeans = KMeans(n_clusters = 5, random_state=0).fit(X)
    counts = np.bincount(kmeans.labels_)
    lat, lon = tuple(kmeans.cluster_centers_[np.argmax(counts)])
    
    db_conn, db_c = access_db()
    db_c.execute("UPDATE users SET home_lat = ?, home_lon = ? WHERE id = ?", (str(lat), str(lon), str(msg_id)))
    db_conn.commit()

    release_db(db_conn)

def has_home(msg_id):
    db_conn, db_c = access_db()
    
    db_c.execute("SELECT home_lat, home_lon FROM users WHERE id = ?", (str(msg_id)))
    response = db_c.fetchone() 

    release_db(db_conn)

    return (response != (None, None))

def can_find_home(msg_id):
    db_conn, db_c = access_db()

    db_c.execute("SELECT id FROM data WHERE id = ?", (str(msg_id)))
    response = db_c.fetchall()

    release_db(db_conn)

    return len(response) > HOME_THRESHOLD

def update_all():
    db_conn, db_c = access_db()

    db_c.execute("SELECT id FROM users")
    response = [x[0] for x in db_c.fetchall()]

    release_db(db_conn)

    for msg_id in response:
        update_scores(msg_id)

    threading.Timer(UPDATE_TIME_WAIT_SECONDS, update_all).start()

def update_scores(msg_id):
    _has_home = has_home(msg_id)
    for time_diff in [1,7,14,30]:
        people_near = num_people_near(msg_id, time_diff)
        left_home = 0
        if _has_home:
            left_home = times_left_home(msg_id, time_diff)
        update_score(msg_id, people_near, left_home, time_diff)

def update_score(msg_id, people_near, left_home, time_diff):
    db_conn, db_c = access_db()

    times = {1: "day", 7: "week", 14: "two_week", 30: "month"}

    query = "UPDATE users SET " + times[time_diff] + "_trips = " + str(left_home) + ", " + times[time_diff] + "_people = " + str(people_near) + " WHERE id = " + str(msg_id)
    db_c.execute(query)
    db_conn.commit()

    release_db(db_conn)

def get_demo_score(time_diff, age_min, age_max, sex):
    db_conn, db_c = access_db()

    times = {1: "day", 7: "week", 14: "two_week", 30: "month"}

    query = "SELECT " + times[time_diff] + "_trips, " + times[time_diff] + "_people FROM users WHERE age >= " + str(age_min) + " AND age <= " + str(age_max) + ((" AND sex = '" + str(sex) + "'")  if sex != "all" else "")
    db_c.execute(query)
    scores = db_c.fetchall()

    release_db(db_conn)

    if (len(scores) != 0):
        trips, people = zip(*scores)
    else:
        trips = [0]
        people = [0]
    avg_trips = sum(trips) / len(trips)
    avg_people = sum(people) / len(people)
    avg_score = (avg_people * 3) + avg_trips
    return (avg_score, avg_trips, avg_people)

def get_score(msg_id, time_diff):
    people_near = num_people_near(msg_id, time_diff)
    left_home = 0
    if has_home(msg_id):
        left_home = times_left_home(msg_id, time_diff)
    update_score(msg_id, people_near, left_home, time_diff)
    return ((people_near * 3) + left_home, left_home, people_near)

def on_client_connect(conn, addr):
    msg = conn.recv(1024).decode("utf-8")[2:] # JAVA client
    #msg = conn.recv(1024).decode("utf-8") # Python client
    print("INCOMING MESSAGE: " + str(msg))
    msg = json.loads(msg)
    if msg['type'] == 'init':
        # FINISHED, NEED TO IMPLEMENT CLIENT SIDE ON 
        age = '' if ('age' not in msg) else msg['age']
        sex = '' if ('sex' not in msg) else msg['sex']

        age = '' if age == -1 else age
        
        db_conn, db_c = access_db()
        db_c.execute("INSERT INTO users (age, sex) VALUES (?, ?)", (age, sex))
        ret_id = db_c.lastrowid
        db_conn.commit()
        release_db(db_conn)
        
        ret_msg = {
            "type":"init_response",
            "id": ret_id
        }

        ret_msg = json.dumps(ret_msg)
        conn.send(ret_msg.encode("utf-8"))
    elif msg['type'] == 'insert':
        # NEED TO FIGURE OUT MAIN DB TABLE SCHEMA FIRST
        msg_id = msg['id']
        msg_time = datetime.datetime.now()
        msg_lat = msg['lat']
        msg_lon = msg['lon']

        db_conn, db_c = access_db()
        db_c.execute("INSERT INTO data(id, time, lat, lon) VALUES (?, ?, ?, ?)", (msg_id, msg_time, msg_lat, msg_lon))
        db_conn.commit()
        release_db(db_conn)

        if not has_home(msg_id):
            if can_find_home(msg_id):
                set_home(msg_id)
                
    elif msg['type'] == 'query':
        if msg['query'] == 'get_score':
            score = get_score(msg['id'], msg['time_diff'])
            score = [0 if x == None else x for x in score]
            ret_msg = {
                "type": "query_response",
                "score": score[0],
                "trips": score[1],
                "people": score[2]
            }
            ret_msg = json.dumps(ret_msg) + "Q"
            conn.send(ret_msg.encode("utf-8"))
        elif msg['query'] == 'get_trips':
            if has_home(msg['id']):
                trips = get_trips(msg['id'], msg['time_diff'])
                trips = 0 if trips == [] else trips
                if trips != 0:    
                    times, lat, lon = zip(*trips)
                    coords = list(zip(lat,lon))
                else:
                    times = 0;
                    coords = 0;
                ret_msg = {
                    "type": "query_repsonse",
                    "trips": coords,
                    "times": times
                }
                ret_msg = json.dumps(ret_msg) + "Q"
                conn.send(ret_msg.encode("utf-8"))
            else:
                conn.send("Q".encode("utf-8"))
        elif msg['query'] == 'get_num_people':
            num_people = num_people_near(msg['id'], msg['time_diff'])
            num_people = 0 if num_people == None else num_people
            ret_msg = {
                "type": "query_response",
                "people": num_people
            }
            ret_msg = json.dumps(ret_msg) + "Q"
            conn.send(ret_msg.encode("utf-8"))
        elif msg['query'] == 'get_demo_score':
            score = get_demo_score(msg['time_diff'], msg['age_min'], msg['age_max'], msg['sex'])
            score = [0 if x == None else x for x in score]
            ret_msg = {
                "type": "query_response",
                "score": score[0],
                "trips": score[1],
                "people": score[2]
            }
            ret_msg = json.dumps(ret_msg) + "Q"
            conn.send(ret_msg.encode("utf-8"))
    conn.close()

print("STARTING SERVER")
HOST = #ENTER IP HERE
PORT = #ENTER PORT HERE
# HOME_THRESHOLD = 864 # 3 Days
HOME_THRESHOLD = 5
DIST_THRESHOLD_HOME = 0.0025
DIST_THRESHOLD_PEOPLE = 0.000025
#UPDATE_TIME_WAIT_SECONDS = 604800 # Update scores every 7 days
UPDATE_TIME_WAIT_SECONDS = 5
print("CREATING LOCK")
db_lock = threading.Lock()
print("UPDATING SCORES")
update_all()
print("OPENING SOCKET")
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind((HOST, PORT))
server_socket.listen(1)
print("LISTENING...")
while True:
    client_socket, address = server_socket.accept()
    print("CONNECTION!");
    threading.Thread(target=on_client_connect, args=(client_socket, address)).start()