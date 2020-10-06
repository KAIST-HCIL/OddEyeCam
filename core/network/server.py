#!/usr/bin/python           # This is server.py file

import socket               # Import socket module
import ast
import numpy as np
import time
import collections

gravdeq = collections.deque([])

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)         # Create a socket object
s.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
host = socket.gethostname() # Get local machine name
c = None

isStop = False

def mysend(coord):
    if not( 'nan' in coord):
        c.send(coord.encode())

def stop_server():
    global isStop
    isStop = True

def get_grav():
    if gravdeq:
        grav = gravdeq.pop()
        gravdeq.clear()
    else:
        grav = np.array([0,0,0])
    return grav

def open_server(port=5000,BUF_SIZE = 1024):
    global c
    print('[Server]','Server is opened!')
    s.bind((host, port))        # Bind to the port
    s.listen(5)                 # Now wait for client connection.
    c, addr = s.accept()     # Establish connection with client.
    s.setblocking(0)
    c.setblocking(0)
    print ('Got connection from', addr)
    while True:
        time.sleep(0.0001)
        if isStop:
            c.close()
            print("A server is closed.")
            break
        try:
            data = c.recv(BUF_SIZE)
            msg = data.decode()
        except:
            continue
        try:
            msg_start = msg.split('START')
            msg_end = msg_start[1].split('END')
            accData = ast.literal_eval(msg_end[0])
        except:
            print("Refresh Buffer:",msg)
            continue
        gravdeq.append(np.array([accData['x'], accData['y'], accData['z']]))