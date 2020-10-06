from util.fisheyecam_streaming import FisheyeCam
from util.realsense_streaming import DepthCam
from util.visualize3d import visualize_3d
from core.oddeyecam import OddEyeCam
import core.network.server as server
import threading
import cv2
import numpy as np
import time
import collections
from core.math_tool.oneeurofilter import OneEuroFilter
from core.math_tool.coordinate_system import CoordSys

# -- Open Camera --
cam = FisheyeCam()
depth_cam = DepthCam()
ec = OddEyeCam()

# -- Open Server --
t = threading.Thread(target=server.open_server)
t.start()

# --    Queue   --
coorddeq = collections.deque([])

def show_webcam():
    while True:
        # Camera Load
        startTime = time.time()
        depth_img, color_img = depth_cam.get_img() # depth, RGB
        ret, fisheye_img = cam.get_img() # fisheye
        verts = depth_cam.get_pointcloud()
        if not ret:
            continue

        grav = server.get_grav()
        if np.all(grav == np.array([0,0,0])):
            continue
        ec.run_oddeyecam(fisheye_img, color_img, verts, grav)
        ref = ec.get_view_from_chest_of('chest')
        pred = ec.get_view_from_chest_of('phone')
        server.mysend(pred.to_string())
        endTime = time.time() - startTime
        
        # Display Camera
        img1 = ec.draw_keypoints(ec.equirectangular_img,ec.keypoints)
        img2 = ec.draw_keypoints(color_img,ec.depth_coord)
        img3 = ec.draw_keypoints(ec.depthframe_img,ec.depth_coord)
        depth_colormap = cv2.applyColorMap(cv2.convertScaleAbs(depth_img, alpha=0.03), cv2.COLORMAP_JET)
        img4 = ec.draw_keypoints(depth_colormap,ec.depth_coord)
        img5 = ec.draw_keypoints(ec.perspective_img,ec.depth_coord)

        cv2.imshow('WFoV (equi)',img1)
        cv2.imshow('RealSense RGB',img2)
        cv2.imshow('transformed WFoV',img3)
        cv2.imshow('RealSense Depth',img4)

        key = cv2.waitKey(1)
        if key == 27:
            print("[CV Keypress]","User pressed exit button")
            server.stop_server()
            break  # esc to quit
        elif key == ord('a'):
            update_beta(down=True)
        elif key == ord('s'):
            update_beta(up=True)
        elif key == ord('q'):
            update_fcmin(down=True)
        elif key == ord('w'):
            update_fcmin(up=True)
        elif key == 32:
            cv2.imwrite('./captured_img/fisheye.jpg',fisheye_img)
            cv2.imwrite('./captured_img/equirectangular_img.jpg',img1)
            cv2.imwrite('./captured_img/d415_RGB.jpg',color_img)
            cv2.imwrite('./captured_img/d415_Depth.jpg',img4)
            cv2.imwrite('./captured_img/depthframe_img.jpg',ec.depthframe_img)
            print("Images are captured") # spacebar to capture image

def main():
    show_webcam()
    cam.release()
    print("[System]","Finished!")

if __name__ == '__main__':
    main()