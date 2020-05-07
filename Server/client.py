import socket
import json

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

host = "192.168.0.9"
port = 5905

s.connect((host, port))

msg = {
    "type":"test",
    "id": 1,
    "time_diff": 1
}
msg = json.dumps(msg)
s.send(msg.encode("utf-8"))
# msg = json.loads(s.recv(1024).decode('utf-8'))
# print(msg)

s.close()
