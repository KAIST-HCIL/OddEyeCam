"""
Written by DaehwaKim
>> daehwa.github.io
"""
import numpy as np
import cv2
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from load_param import cap, cap2, frame_from, frame_to, rs_offset, opti_offset

plt.ion()
fig = plt.figure(figsize=(8, 8))
ax = fig.add_subplot(111, projection='3d') # Axe3D object
r_start, r_end = -500,500
visShPoints, = ax.plot(range(r_start,r_end), range(r_start,r_end), range(r_start,r_end),alpha=0.6, linestyle="", marker=".", c='g')
visNosePoints, = ax.plot(range(r_start,r_end), range(r_start,r_end), range(r_start,r_end),alpha=0.6, linestyle="", marker=".", c='y')
visTruePoints, = ax.plot(range(r_start,r_end), range(r_start,r_end), range(r_start,r_end),alpha=0.6, linestyle="", marker=".", c='b')
visPredPoints, = ax.plot(range(r_start,r_end), range(r_start,r_end), range(r_start,r_end),alpha=0.6, linestyle="", marker=".", c='r')
xaxis, yaxis, zaxis = np.array([[-400,0,0],[400,0,0]]), np.array([[0,-400,0],[0,400,0]]), np.array([[0,0,-400],[0,0,400]])
visBodyAxisX, = ax.plot(xaxis[:,0], xaxis[:,1], xaxis[:,2],alpha=0.6, c='r')
visBodyAxisY, = ax.plot(yaxis[:,0], yaxis[:,1], yaxis[:,2],alpha=0.6, c='g')
visBodyAxisZ, = ax.plot(zaxis[:,0], zaxis[:,1], zaxis[:,2],alpha=0.6, c='b')
visCamAxisX, = ax.plot(xaxis[:,0], xaxis[:,1], xaxis[:,2],alpha=0.6, c='r')
visCamAxisY, = ax.plot(yaxis[:,0], yaxis[:,1], yaxis[:,2],alpha=0.6, c='g')
visCamAxisZ, = ax.plot(zaxis[:,0], zaxis[:,1], zaxis[:,2],alpha=0.6, c='b')
visTruthAxisX, = ax.plot(xaxis[:,0], xaxis[:,1], xaxis[:,2],alpha=0.6, c='r')
visTruthAxisY, = ax.plot(yaxis[:,0], yaxis[:,1], yaxis[:,2],alpha=0.6, c='g')
visTruthAxisZ, = ax.plot(zaxis[:,0], zaxis[:,1], zaxis[:,2],alpha=0.6, c='b')
ax.set_xlabel('$x$',); ax.set_ylabel('$y$'); ax.set_zlabel('$z$')

def visualize_3d(opti_obj, ec_obj):
    t = opti_obj.center
    p = ec_obj.center_phone

    # distance = t - p
    # print("error:",distance," (mm)")

    Shpt, Nose = ec_obj.ShoulderPts_inlier, ec_obj.center_phone
    xunit, yunit, zunit = ec_obj.xunit_phone, ec_obj.yunit_phone, ec_obj.zunit_phone
    xaxis, yaxis, zaxis = np.array([p, p + xunit*100]), np.array([p, p + yunit*100]), np.array([p, p + zunit*100])
    xunit_, yunit_, zunit_ = opti_obj.x_axis, opti_obj.y_axis, opti_obj.z_axis
    xaxis_, yaxis_, zaxis_ = np.array([t, t + xunit_*100]), np.array([t, t + yunit_*100]), np.array([t, t + zunit_*100])
    visShPoints.set_data(Shpt[:,0], Shpt[:,1])
    visShPoints.set_3d_properties(Shpt[:,2], zdir="z")
    visNosePoints.set_data(Nose[0], Nose[1])
    visNosePoints.set_3d_properties(Nose[2], zdir="z")
    visTruePoints.set_data(t[0], t[1])
    visTruePoints.set_3d_properties(t[2], zdir="z")
    visPredPoints.set_data(p[0], p[1])
    visPredPoints.set_3d_properties(p[2], zdir="z")
    visCamAxisX.set_data(xaxis[:,0], xaxis[:,1])
    visCamAxisX.set_3d_properties(xaxis[:,2], zdir="z")
    visCamAxisY.set_data(yaxis[:,0], yaxis[:,1])
    visCamAxisY.set_3d_properties(yaxis[:,2], zdir="z")
    visCamAxisZ.set_data(zaxis[:,0], zaxis[:,1])
    visCamAxisZ.set_3d_properties(zaxis[:,2], zdir="z")
    visTruthAxisX.set_data(xaxis_[:,0], xaxis_[:,1])
    visTruthAxisX.set_3d_properties(xaxis_[:,2], zdir="z")
    visTruthAxisY.set_data(yaxis_[:,0], yaxis_[:,1])
    visTruthAxisY.set_3d_properties(yaxis_[:,2], zdir="z")
    visTruthAxisZ.set_data(zaxis_[:,0], zaxis_[:,1])
    visTruthAxisZ.set_3d_properties(zaxis_[:,2], zdir="z")
    fig.canvas.draw()
    fig.canvas.flush_events()

if not(cap.isOpened()):
    print("No video source")
    exit()
frames = []
frames2 = []
needupdate = True
for i in range(0,rs_offset):
    cap2.read()
ret, frame = cap.read()
ret2, frame2 = cap2.read()
frames.append(frame)
frames2.append(frame2)
i = 0
offset = np.array([0,0,0])
def streaming(opti_obj, ec_obj):
    global i, needupdate, frames, frames2, frame, frame2
    key = cv2.waitKey(2)
    # if(i == offset_sampl_frame):
    #     global offset
    #     offset = ec_obj.center_phone - opti_obj.center
    if(opti_obj == None or not ec_obj.valid or i < frame_to):
        i = i+1
        ret, frame = cap.read()
        ret2, frame2 = cap2.read()
        frames.append(frame)
        frames2.append(frame2)
        needupdate = True
        return
    if key == ord('d') or i < frame_to:
        i = i+1
        if(len(frames) > i):
            frame = frames[i]
            frame2 = frames2[i]
        else:
            ret, frame = cap.read()
            ret2, frame2 = cap2.read()
            frames.append(frame)
            frames2.append(frame2)
        needupdate = True
        print("Current Frame:"+str(i))
        # x_angle, _ = angleNaxis(opti_obj.x_axis,ec_obj.phone_xunit)
        # y_angle, _ = angleNaxis(opti_obj.y_axis,ec_obj.phone_yunit)
        # z_angle, _ = angleNaxis(opti_obj.z_axis,ec_obj.phone_zunit)
        # print(x_angle,y_angle,z_angle)
        needupdate = True
    elif key == ord('a'):
        i = i-1
        frame = frames[i]
        frame2 = frames2[i]
        needupdate = True
        print("Current Frame:"+str(i))
    if(needupdate):
        visualize_3d(opti_obj,ec_obj)
        if not(len(frames) > i):
            frames.append(frame)
            frames2.append(frame2)
    needupdate = False

def get_frame_num():
    return i

def get_images():
    return frames[i], frames2[i]