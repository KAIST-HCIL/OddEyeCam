import os
import sys
from core.img_tool.warp import Warper
from core.img_tool.remap import Remapper
from core.thirdparty.load_openpose import PoseEstimator
from core.math_tool.geometric_tool import GeoTool
from core.math_tool.coordinate_system import CoordSys
import cv2
import numpy as np
from core.math_tool.oneeurofilter import OneEuroFilter

mincutoff_, beta_ = 0.005, 0.005
f_phone = OneEuroFilter(mincutoff = mincutoff_, beta = beta_)
f_camera = OneEuroFilter()
gtool = GeoTool()
pe = PoseEstimator()
warper = Warper()
remapper = Remapper()

class OddEyeCam():
    def __init__(self):
        """
        These objects are view from global frame,
        which is Intel Realsense Camera
        """
        self.camera = self._set_camera_axis()
        self.phone = self._set_phone_axis()
        self.chest = CoordSys(ref_coordsys = self.camera)
        self.remapper = remapper
        
    def run_oddeyecam(self,fisheye_img, rs_img, verts, grav):
        self.fisheye_img = fisheye_img
        self.rs_img = rs_img
        self.verts = verts
        self.grav = grav
        self.equirectangular_img, self.perspective_img, self.depthframe_img = warper.run_warper(fisheye_img)
        self.keypoints = pe.find_body_on_2D(self.equirectangular_img,verts)
        self.depth_coord = remapper.run_remapper(self.keypoints,self.verts)
        self.per_coord = remapper.per_coord
        # self.rs_img = remapper.mark_valid_region(rs_img)
        # self.rs_img = remapper.mark_chest_region(rs_img)
        self._set_keyponts_3D(self.keypoints)
        self._set_chest()
        self._to_chest_frame()

    """
    Filter
    """
    def one_euro_filter(self,filter,obj):
        center = obj.center
        if not np.isnan(center).any():
            obj.center = filter(center)

    # def update_beta(self, up=False, down=False):
    #     step = 0.001
    #     global mincutoff_, beta_, f_phone
    #     if beta_ - step < 0 and down==True:
    #         print("beta is already 0")
    #         return
    #     if up:
    #         beta_ = beta_ + step
    #     elif down:
    #         beta_ = beta_ - step
    #     beta_ - round(beta_,3)
    #     print("beta:",beta_)
    #     f_phone = OneEuroFilter(mincutoff = mincutoff_, beta = beta_)

    # def update_fcmin(self, up=False, down=False):
    #     step = 0.001
    #     global mincutoff_, beta_, f_phone
    #     if mincutoff_ - step < 0 and down==True:
    #         print("fc_min is already 0")
    #         return
    #     if up:
    #         mincutoff_ = mincutoff_ + step
    #     elif down:
    #         mincutoff_ = mincutoff_ - step
    #     mincutoff_ - round(mincutoff_,3)
    #     print("fc_min:",mincutoff_)
    #     f_phone = OneEuroFilter(mincutoff = mincutoff_, beta = beta_)

    """
    CoordSys1 view from CoordSys2
    """
    def _to_chest_frame(self):
        # New camera
        self.camera_view_from_chest = gtool.get_view_from(self.camera, view_to=self.chest)
        # New phone
        self.phone_view_from_chest = gtool.get_view_from(self.phone, view_to=self.chest)
        # New chest
        self.chest_view_from_chest = gtool.get_view_from(self.chest, view_to=self.chest)
        # Polar Expression
        gtool.get_polar_expression_of_chest(self.phone_view_from_chest)
        # gtool.get_polar_expression_of_chest(self.camera_view_from_chest)

    def get_view_from_chest_of(self,name):
        if name == 'chest':
            return self.chest_view_from_chest
        elif name == 'phone':
            # self.one_euro_filter(f_phone,self.phone_view_from_chest)
            return self.phone_view_from_chest
        elif name == 'camera':
            # self.one_euro_filter(f_camera,self.camera_view_from_chest)
            return self.camera_view_from_chest

    def get_view_from_camera_of(self,name):
        if name == 'chest':
            return self.chest
        elif name == 'phone':
            return self.phone
        elif name == 'camera':
            return self.camera

    """
    Camera
    Esimation
    """
    def _set_camera_axis(self):
        origin = np.array([0,0,0])
        x_axis = np.array([1,0,0])
        y_axis = np.array([0,1,0])
        z_axis = np.array([0,0,1])
        camera_sys = CoordSys(origin,x_axis,y_axis,z_axis)
        camera_sys.set_ref_coordsys(camera_sys)
        return camera_sys

    """
    Phone
    Esimation
    """
    def _set_phone_axis(self):
        origin = self.camera.get_center()
        x =  self.camera.get_x_axis()
        y = self.camera.get_y_axis()
        z = self.camera.get_z_axis()
        x_axis = -x
        y_axis = gtool.rotation(x, 40, -y)
        z_axis = gtool.rotation(x, 40, z)
        center = origin - 90 * y_axis
        phone_sys = CoordSys(center,x_axis,y_axis,z_axis,self.camera)
        return phone_sys

    """
    Chest
    Esimation
    """
    def get_valid_shoulder_points(self,points):
        d = np.sqrt(points[:,0]**2+points[:,1]**2+points[:,2]**2)
        points = points[np.all([points[:,2]>0,d>50,d<1000],axis=0)]
        return points

    def _set_chest_center(self):
        # center = (self.get_right_shoulder() + self.get_left_shoulder())/2
        center = self.get_neck()
        self.chest.set_center(center)
        return center

    def _set_chest_x_axis(self):
        right_shoulder = remapper.get_right_shoulder_2D()
        left_shoulder = remapper.get_left_shoulder_2D()
        idx = gtool.get_points_on_2D_line(right_shoulder, left_shoulder)
        idx = remapper.check_overshoot(idx)
        v, u = idx[:,0],idx[:,1]
        points = self.verts[v,u,:]*1000 # to mm unit
        points = self.get_valid_shoulder_points(points)
        x_axis = gtool.linear_regression(points)
        self.chest.set_x_axis(x_axis)
        return x_axis

    def _set_chest_y_axis(self):
        grav = -self.grav
        y_axis = gtool.vec2frame(self.phone, grav, self.camera, is_point=False)
        y_axis = gtool.unitvec(y_axis)
        self.chest.set_y_axis(y_axis)
        return y_axis

    def _set_chest_z_axis(self):
        x_axis = self.chest.get_x_axis()
        y_axis = self.chest.get_y_axis()
        # orthogonalize x and y-axis
        x_axis =  x_axis - np.dot(x_axis,y_axis)*y_axis
        x_axis = gtool.unitvec(x_axis)
        self.chest.set_x_axis(x_axis)
        # z-axis from cross product
        z_axis = np.cross(x_axis,y_axis)
        z_axis = gtool.unitvec(z_axis)
        self.chest.set_z_axis(z_axis)
        return z_axis

    def _set_chest(self):
        self._set_chest_center()
        self._set_chest_x_axis()
        self._set_chest_y_axis()
        self._set_chest_z_axis()

    """
    Keypoints
    related
    """
    def draw_keypoints_beauty(self,img,keypoints):
        image = img.copy()  
        red_color = [0,0,255]
        green_color = [0,255,0]
        orange_color = [10,69,250]
        for k in keypoints:
            cv2.circle(image, (k[1], k[0]), (2), red_color, 2)
        neck = keypoints[1,:]
        lsh = keypoints[5,:]
        rsh = keypoints[2,:]
        cv2.circle(image, (rsh[1], rsh[0]), (4), orange_color, -1)
        cv2.circle(image, (lsh[1], lsh[0]), (4), green_color, -1)
        cv2.line(image, (neck[1], neck[0]), (lsh[1], lsh[0]), green_color, 2)
        cv2.line(image, (neck[1], neck[0]), (rsh[1], rsh[0]), orange_color, 2)
        cv2.circle(image, (neck[1], neck[0]), (4), red_color, -1)
        return image

    def draw_keypoints(self,img,keypoints):
        for k in keypoints:
            cv2.circle(img, (k[1], k[0]), (2), [0,0,255], 10)
        return img

    def draw_shoulders(self,img):
        rsh = self.right_shoulder_idx
        lsh = self.left_shoulder_idx
        # rsh = remapper.get_right_shoulder_2D()
        # lsh = remapper.get_left_shoulder_2D()
        cv2.circle(img, (rsh[1],rsh[0]), (2), [0,0,255], 2)
        cv2.circle(img, (lsh[1],lsh[0]), (2), [255,0,0], 2)
        return img

    def _set_keyponts_3D(self,keypoints_2D):
        right_shoulder_idx = remapper.get_right_shoulder_2D()
        left_shoulder_idx = remapper.get_left_shoulder_2D()
        neck_idx = remapper.get_neck_2D()
        self.right_shoulder = self.verts[right_shoulder_idx[0],right_shoulder_idx[1],:]
        self.left_shoulder = self.verts[left_shoulder_idx[0],left_shoulder_idx[1],:]
        self.neck = self.verts[neck_idx[0],neck_idx[1],:]

    def get_right_shoulder(self):
        return self.right_shoulder*1000 # to mm unit

    def get_left_shoulder(self):
        return self.left_shoulder*1000 # to mm unit

    def get_neck(self):
        return self.neck*1000 # to mm unit

    def get_shoulders(self):
        rsh = self.get_right_shoulder()
        lsh = self.get_left_shoulder()
        shoudlers = np.array([rsh,lsh])
        return shoudlers
