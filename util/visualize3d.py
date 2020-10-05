import os
import sys
from core.math_tool.coordinate_system import CoordSys
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import numpy as np
import cv2

def _update_element(obj,data,is_Point=False):
    if is_Point:
        obj.set_data(data[0], data[1])
        obj.set_3d_properties(data[2], zdir="z")
    else:
        obj.set_data(data[:,0], data[:,1])
        obj.set_3d_properties(data[:,2], zdir="z")

def _update(obj_list,data,length=100):
    pos = data.center
    x_axis = np.array([pos, pos + data.x_axis*length])
    y_axis = np.array([pos, pos + data.y_axis*length])
    z_axis = np.array([pos, pos + data.z_axis*length])
    _update_element(obj_list[0],pos,is_Point=True)
    _update_element(obj_list[1],x_axis)
    _update_element(obj_list[2],y_axis)
    _update_element(obj_list[3],z_axis)

def visualize_3d(ref_,pred_,truth=None,user_exit=False):
    # Plot Configure
    plt.ion()
    fig = plt.figure(figsize=(8, 8))
    ax = fig.add_subplot(111, projection='3d') # Axe3D object
    ax.set_xlabel('$x$',); ax.set_ylabel('$y$'); ax.set_zlabel('$z$')
    ax.view_init(elev=120, azim=60)
    ax.dist = 10

    r_start, r_end = -1,1
    x_axis_, y_axis_, z_axis_ = np.array([[-400,0,0],[400,0,0]]), np.array([[0,-400,0],[0,400,0]]), np.array([[0,0,0],[0,0,800]])
    # Reference Object
    visRefPoints, = ax.plot(range(r_start,r_end),
                            range(r_start,r_end),
                            range(r_start,r_end),
                            alpha=1, linestyle="", marker=".", c='g')
    visRefAxisX, = ax.plot(x_axis_[:,0], x_axis_[:,1], x_axis_[:,2],alpha=0.6, c='r')
    visRefAxisY, = ax.plot(y_axis_[:,0], y_axis_[:,1], y_axis_[:,2],alpha=0.6, c='g')
    visRefAxisZ, = ax.plot(z_axis_[:,0], z_axis_[:,1], z_axis_[:,2],alpha=0.6, c='b')

    # Phone Object
    visPredPoints, = ax.plot(range(r_start,r_end),
                            range(r_start,r_end),
                            range(r_start,r_end),
                            alpha=0.6, linestyle="", marker=".", c='r')
    visPredAxisX, = ax.plot(x_axis_[:,0], x_axis_[:,1], x_axis_[:,2],alpha=0.6, c='r')
    visPredAxisY, = ax.plot(y_axis_[:,0], y_axis_[:,1], y_axis_[:,2],alpha=0.6, c='g')
    visPredAxisZ, = ax.plot(z_axis_[:,0], z_axis_[:,1], z_axis_[:,2],alpha=0.6, c='b')

    visTruthPoints, = ax.plot(range(r_start,r_end),
                            range(r_start,r_end),
                            range(r_start,r_end),
                            alpha=0.6, linestyle="", marker=".", c='b')
    visTruthAxisX, = ax.plot(x_axis_[:,0], x_axis_[:,1], x_axis_[:,2],alpha=0.6, c='r')
    visTruthAxisY, = ax.plot(y_axis_[:,0], y_axis_[:,1], y_axis_[:,2],alpha=0.6, c='g')
    visTruthAxisZ, = ax.plot(z_axis_[:,0], z_axis_[:,1], z_axis_[:,2],alpha=0.6, c='b')

    # Visualization Object List
    ref_vis = [visRefPoints, visRefAxisX, visRefAxisY, visRefAxisZ]
    pred_vis = [visPredPoints, visPredAxisX, visPredAxisY, visPredAxisZ]
    truth_vis = [visTruthPoints, visTruthAxisX, visTruthAxisY, visTruthAxisZ]
    while True:
        ref,pred = ref_[0],pred_[0]
        if ref is None or pred is None:
            continue
        if user_exit:
            exit()
        # _update(ref_vis,ref,length=800)
        _update(pred_vis,pred)
        if not truth is None:
            _update(truth_vis,truth)
        fig.canvas.draw()
        fig.canvas.flush_events()