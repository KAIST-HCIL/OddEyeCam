import pandas as pd
import numpy as np
import cv2
import os
import sys
this_dir = os.path.dirname(__file__)
this_dir = os.path.join(this_dir,'cam_params')

print("[Load Param]","Loading Data...")

# [Camera Information]
fisheye_size = [640,480]
rscam_size = [424, 240]
rscam_depth_size = [480, 270]
exposure = -5
gain = 50
win_M, win_N = int(fisheye_size[0]/1.3), int(fisheye_size[1]/1.3)
max_depth = 1.5
UINT_MAX = 65536
# [Camera Information]

# [Mapping Parameters]
equi_u = pd.read_csv(this_dir+"/equi_u.csv", delimiter=",", dtype=np.float32, header=None).values
equi_v = pd.read_csv(this_dir+"/equi_v.csv", delimiter=",", dtype=np.float32, header=None).values
per_u = pd.read_csv(this_dir+"/per_u.csv", delimiter=",", dtype=np.float32, header=None).values
per_v = pd.read_csv(this_dir+"/per_v.csv", delimiter=",", dtype=np.float32, header=None).values
per_u_rev = pd.read_csv(this_dir+"/per_u_rev.csv", delimiter=",", dtype=np.float32, header=None).values
per_v_rev = pd.read_csv(this_dir+"/per_v_rev.csv", delimiter=",", dtype=np.float32, header=None).values
fs = cv2.FileStorage(this_dir+"/homography_per2depth.xml", cv2.FILE_STORAGE_READ)
homography = fs.getFirstTopLevelNode().mat()
M, N= fisheye_size[0], fisheye_size[1]
# [Mapping Parameters]

print("[Load Param]","Complete to Load Data!")