import cv2
import numpy as np
import os
import sys
this_dir = os.path.dirname(__file__)
sys.path.append(this_dir)
import params as prm

class Remapper():
    def __init__(self):
        self.right_shoulder_idx = 2
        self.left_shoulder_idx = 5
        self.neck_idx = 1

    def check_overshoot(self, coord, size = prm.rscam_size):
        v_in, u_in = coord[:,0], coord[:,1]
        v_in[v_in >= size[1]] = size[1] - 1
        u_in[u_in >= size[0]] = size[0] - 1
        v_in[v_in < 0] = 0
        u_in[u_in < 0] = 0
        return np.array([v_in, u_in]).astype(np.int).transpose()

    def _move_frame(self,coord,map_u,map_v):
        v, u = coord[:,0], coord[:,1]
        u_moved = np.around(map_u[v,u]).astype(np.int)
        v_moved = np.around(map_v[v,u]).astype(np.int)
        return np.array([v_moved, u_moved]).transpose()

    def _move_depth_frame(self,coord):
        n = coord.shape[0]
        u = np.array([coord[:,1],coord[:,0],np.repeat(1,n)])
        v = np.transpose(np.matmul(prm.homography, u))
        u_moved = np.around(v[:,0]/v[:,2]).astype(np.int)
        v_moved = np.around(v[:,1]/v[:,2]).astype(np.int)
        return np.array([v_moved, u_moved]).transpose()

    def run_remapper(self, keypoints, verts):
        equi_coord = self.check_overshoot(keypoints,prm.fisheye_size)
        # equirectangular frame -> fisheye camera frame
        fisheye_coord = self._move_frame(equi_coord, prm.equi_u, prm.equi_v)
        fisheye_coord = self.check_overshoot(fisheye_coord,prm.fisheye_size)
        # fisheye frame -> perspective camera frame
        per_coord = self._move_frame(fisheye_coord, prm.per_u_rev, prm.per_v_rev)
        per_coord = self.check_overshoot(per_coord,prm.fisheye_size)
        # perspective frame -> depth camera frame
        depth_coord = self._move_depth_frame(per_coord)
        depth_coord = self.check_overshoot(depth_coord,prm.rscam_size)
        # save data
        self.equi_coord = equi_coord
        self.per_coord = per_coord
        self.depth_coord = depth_coord
        # remap the points on zero depth (shadow in depth camera)
        self.remap_invalid_keypoints(verts)
        return self.depth_coord

    def get_right_shoulder_2D(self):
        idx = self.right_shoulder_idx
        return self.depth_coord[idx,:]

    def get_left_shoulder_2D(self):
        idx = self.left_shoulder_idx
        return self.depth_coord[idx,:]

    def get_neck_2D(self):
        idx = self.neck_idx
        return self.depth_coord[idx,:]

    def get_keypoints_2D(self):
        return self.depth_coord

    def _set_right_shoulder_2D(self, val):
        idx = self.right_shoulder_idx
        self.depth_coord[idx,:] = val

    def _set_right_shoulder_2D(self, val):
        idx = self.right_shoulder_idx
        self.depth_coord[idx,:] = val

    def _set_left_shoulder_2D(self, val):
        idx = self.left_shoulder_idx
        self.depth_coord[idx,:] = val

    def _set_neck_2D(self, val):
        idx = self.neck_idx
        self.depth_coord[idx,:] = val

    def _nearest_valid(self,valid_idx,body_coord):
        distance = valid_idx - body_coord
        distance = np.sum(distance**2,axis=1)
        try:
            shortest_idx = np.argmin(distance)
        except:
            return np.array([0,0])
        body_coord_valid = valid_idx[shortest_idx,:]
        u, v = body_coord_valid[1],body_coord_valid[0]
        return np.array([v,u])

    def _edge_valid(self,valid_idx):
        r = self.get_right_shoulder_2D()
        l = self.get_left_shoulder_2D()
        r_x = np.min(valid_idx[valid_idx[:,0] == r[0],1])
        l_x = np.max(valid_idx[valid_idx[:,0] == l[0],1])
        new_r = r
        new_l = np.array([l[0],l_x])
        return new_r,new_l

    def mark_valid_region(self, img):
        idx = self.valid_idx
        color_white = np.array([255,255,255])
        img[idx[:,0],idx[:,1],:] = color_white # mark the region
        return img

    def get_chest_region(self):
        r = self.get_right_shoulder_2D()
        l = self.get_left_shoulder_2D()
        condition = [self.valid_idx[:,0]>l[0],self.valid_idx[:,0]>r[0]]
        # condition = [self.valid_idx[:,0]>l[0],self.valid_idx[:,0]>r[0],self.valid_idx[:,1]<l[1],self.valid_idx[:,1]>r[1]]
        idx = self.valid_idx[np.all(condition,axis=0)]
        return idx

    def mark_chest_region(self, img):
        idx = self.get_chest_region()
        color_white = np.array([255,255,255])
        img[idx[:,0],idx[:,1],:] = color_white # mark the region
        return img

    def remap_invalid_keypoints(self, verts):
        d = np.sqrt(verts[:,:,0]**2+verts[:,:,1]**2+verts[:,:,2]**2)
        condition = [d>0, d<0.8, verts[:,:,2]>0.05]
        valid_idx = np.argwhere(np.all(condition,axis=0))
        self.valid_idx = valid_idx
        # right_shoulder_valid, left_shoulder_valid = self._edge_valid(valid_idx)
        right_shoulder_valid = self._nearest_valid(valid_idx,self.get_right_shoulder_2D())
        left_shoulder_valid = self._nearest_valid(valid_idx,self.get_left_shoulder_2D())
        neck_valid = self._nearest_valid(valid_idx,self.get_neck_2D())
        self.before_right_shoulder =  np.copy(self.get_right_shoulder_2D())
        self._set_right_shoulder_2D(right_shoulder_valid)
        self._set_left_shoulder_2D(left_shoulder_valid)
        self._set_neck_2D(neck_valid)

    def get_before_right_shoulder_2D(self, verts):
        is_invalid = False
        rshi = self.before_right_shoulder
        d = np.sqrt(verts[rshi[0],rshi[1],0]**2+verts[rshi[0],rshi[1],1]**2+verts[rshi[0],rshi[1],2]**2)
        condition = [d>0, d<0.8, verts[rshi[0],rshi[1],2]>0.05]
        if not np.all(condition,axis=0):
            is_invalid = True
        return rshi, is_invalid