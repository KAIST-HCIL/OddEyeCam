import cv2
import numpy as np

class FisheyeCam():
    def __init__(self):
        ## Fisheye Cam
        self.cam = cv2.VideoCapture(1 + 700)
        if not self.cam.isOpened():
            print("[CV Camera]","Warning: unable to open camera")
            print("[CV Camera]","fisheye camera: " + str(self.cam.isOpened()))
            exit()
        print("[CV Camera]","Cameras are Opened")
        ## Setting
        self.cam.set(cv2.CAP_PROP_SETTINGS,0)
        # self.cam.set(cv2.CAP_PROP_EXPOSURE,-5)
        ## Delayed queue
        ret, img = self.cam.read()
        self.cam_queue = [ret, img]

    def get_img_experiment(self):
        ret, img = self.cam.read()
        return ret, img

    def get_img(self): # for live
        ret_next, img_next = self.cam.read()
        img = self.cam_queue.pop()
        ret = self.cam_queue.pop()
        self.cam_queue.extend([ret_next, img_next])
        return ret, img

    def release(self):
        self.cam.release()