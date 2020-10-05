import cv2
import numpy as np
import pyrealsense2 as rs
from core.img_tool.params import rscam_depth_size, rscam_size, UINT_MAX

class DepthCam():
    def __init__(self):
        #realsense d415
        self.pipeline = rs.pipeline()
        config = rs.config()
        config.enable_stream(rs.stream.depth, rscam_depth_size[0], rscam_depth_size[1], rs.format.z16, 30) # does not work in usb 2.0
        config.enable_stream(rs.stream.color, rscam_size[0], rscam_size[1], rs.format.bgr8, 30)
        profile = self.pipeline.start(config)
        depth_sensor = profile.get_device().first_depth_sensor()
        depth_scale = depth_sensor.get_depth_scale()
        print("[Realsense]","Depth Scale is: " , depth_scale)
        clipping_distance_in_meters = 1 # 1 meter
        clipping_distance = clipping_distance_in_meters / depth_scale
        align_to = rs.stream.color
        self.align = rs.align(align_to)
        self.pc = rs.pointcloud()

    def get_img(self):
        frames = self.pipeline.wait_for_frames()
        # depth align
        aligned_frames = self.align.process(frames)
        self.depth_frame = aligned_frames.get_depth_frame()
        self.color_frame = aligned_frames.get_color_frame()
        # self.depth_frame = frames.get_depth_frame()
        # self.color_frame = frames.get_color_frame()
        self.depth_img = np.asanyarray(self.depth_frame.get_data()) # aligned_depth_frame
        self.color_img = np.asanyarray(self.color_frame.get_data())
        return self.depth_img, self.color_img

    def get_pointcloud(self):
        points = self.pc.calculate(self.depth_frame)
        self.pc.map_to(self.color_frame)
        v = points.get_vertices()
        self.verts = np.asanyarray(v).view(np.float32).reshape(rscam_size[1],rscam_size[0], 3)  # xyz
        return self.verts

    def get_norm_depth_img(self):
        return self.depth_img/UINT_MAX

    def get_depth_img_from_pointcloud(self):
        v = self.get_pointcloud()
        d = np.sqrt(v[:,:,0]*v[:,:,0]+v[:,:,1]*v[:,:,1]+v[:,:,2]*v[:,:,2])
        d[d>max_depth] = max_depth
        depth_img_from_pc  = d/max_depth
        return depth_img_from_pc

    def print_physical_distance(self):
        a = self.verts[100,212,:]*100
        b = self.verts[134,212,:]*100
        c = a - b
        print(  np.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]),
                np.sqrt(b[0]*b[0]+b[1]*b[1]+b[2]*b[2]),
                np.sqrt(c[0]*c[0]+c[1]*c[1]+c[2]*c[2])  )
