import os
import sys
this_dir = os.path.dirname(__file__)

import numpy as np
openpose_path = os.path.join(this_dir, 'openpose')
op_release_path = os.path.join(openpose_path, 'Release')
model_path = os.path.join(openpose_path, 'models')
print(op_release_path)
sys.path.append(op_release_path);
os.environ['PATH']  = os.environ['PATH'] + ';' + openpose_path + '/x64/Release;' +  openpose_path + '/bin;'
import pyopenpose as op
opWrapper = op.WrapperPython()
params = dict()
params["model_folder"] = model_path
params["number_people_max"] = 1
params["net_resolution"]="-1x160"
params["body"] = 1
params["output_resolution"] = "-1x-1"
params["disable_multi_thread"] = True
opWrapper.configure(params)
opWrapper.start()

class PoseEstimator():
    def __init__(self):
        self.RShColor = (0, 140, 255)
        self.LShColor = (0, 255, 215)
        self.NeckColor = (0, 0, 215)
        self.NoseColor = (215, 0, 215)

    def _keypoint_to_index(self,keypoints):
        v = keypoints[:,1]
        u = keypoints[:,0]
        idx = np.array([v,u]).astype(np.int).transpose()
        return idx

    def find_body_on_2D(self, src_img, verts):
        datum = op.Datum()
        datum.cvInputData = src_img
        opWrapper.emplaceAndPop([datum])
        self.op_img = datum.cvOutputData
        #print(datum.poseKeypoints)
        # Check validity
        if not str(datum.poseKeypoints.shape) == '(1, 25, 3)':
            return np.zeros((25, 3)).astype(np.int)
        data = datum.poseKeypoints
        # self.RShoulder2D = np.array([data[0,2,0], data[0,2,1]])
        # self.LShoulder2D = np.array([data[0,5,0], data[0,5,1]])
        # self.Neck2D = np.array([data[0,1,0], data[0,1,1]])
        # keypoint = np.array([self.RShoulder2D, self.LShoulder2D, self.Neck2D]).astype(np.int)
        # return keypoint
        keypoints = data[0]
        # switch (u,v) -> (v,u)
        idx = self._keypoint_to_index(keypoints)
        return idx

    def just_find_body_on_2D(self, src_img):
        datum = op.Datum()
        datum.cvInputData = src_img
        opWrapper.emplaceAndPop([datum])
        self.op_img = datum.cvOutputData
        return datum.cvOutputData, datum.poseKeypoints