import cv2
import numpy as np
import os
import sys
this_dir = os.path.dirname(__file__)
sys.path.append(this_dir)
import params as prm

class Warper():
    def run_warper(self,fisheye_img):
        self.fisheye_img = fisheye_img
        self.equirectangular_img = cv2.remap(self.fisheye_img, prm.equi_u, prm.equi_v, cv2.INTER_LINEAR)
        self.perspective_img = cv2.remap(self.fisheye_img, prm.per_u, prm.per_v, cv2.INTER_LINEAR)
        self.depthframe_img = cv2.warpPerspective(self.perspective_img, prm.homography, (prm.rscam_size[0],prm.rscam_size[1]))
        return self.equirectangular_img,  self.perspective_img, self.depthframe_img