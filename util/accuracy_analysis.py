"""
Written by DaehwaKim
>> daehwa.github.io
"""
import numpy as np
import pickle as pkl
import egocoord as ec
import cv2
from datetime import datetime
import visualize_3d_frame_by_frame as v3dfbf
from load_param import eo, fisheye_size, rscam_size, verts, grav, time, username, frame_from, frame_to, rs_offset, opti_offset, max_depth

#             Origin, Deep Egocoord, Deep Egocoord with depth image
output_type = [False, False, True]

def get_angle_sign(angle, axis, standard):
    axis = axis * standard
    if np.any(axis < 0):
        angle = -angle
    return angle

def angleNaxis(u,v):
    u = u/(np.sqrt(np.dot(u[0],u[0])+np.dot(u[1],u[1])+np.dot(u[2],u[2])))
    v = v/(np.sqrt(np.dot(v[0],v[0])+np.dot(v[1],v[1])+np.dot(v[2],v[2])))
    axis = np.cross(v,u)
    angle = np.arccos(np.dot(u,v))
    return angle, axis

def to_polar_system(o):
    x, y, z = o[0], o[1]+110, o[2]
    d = np.sqrt(x*x+y*y+z*z).astype(np.int)
    theta, axis_theta = angleNaxis(np.array([x,0,z]),np.array([0,0,1]))
    phi, axis_phi = angleNaxis(o,np.array([x,0,z]))
    theta = get_angle_sign(theta, o,np.array([1,0,0]))
    phi = get_angle_sign(phi, o,np.array([0,1,0]))
    theta = np.around(np.degrees(theta),2)
    phi = np.around(np.degrees(phi),2)
    return d, theta, phi

def get_polar_error(opti_obj, ec_obj):
    if not ec_obj.valid:
        return 0, 0, 0
    d_opti, theta_opti, phi_opti = to_polar_system(opti_obj.center)
    d_error = d_opti - ec_obj.d_phone
    theta_error = theta_opti - ec_obj.theta_phone
    phi_error = phi_opti - ec_obj.phi_phone
    return d_error, theta_error, phi_error


fourcc = cv2.VideoWriter_fourcc(*'mp4v')
out_fisheye = cv2.VideoWriter('../result/'+username+'/fisheye_result.mp4',fourcc, 30.0, (fisheye_size[0],fisheye_size[1]), isColor=True)
out_rs_RGB = cv2.VideoWriter('../result/'+username+'/rs_RGB_result.mp4',fourcc, 30.0, (rscam_size[0],rscam_size[1]), isColor=True)
result_gt = np.array([[]]).reshape(-1,3)
result_p = np.array([[]]).reshape(-1,3)
result_gt_axis = np.array([[]]).reshape(-1,9)
result_p_axis = np.array([[]]).reshape(-1,9)
result_input = np.array([[]]).reshape(-1,12)
bodypoint2D = np.array([[]]).reshape(-1,6)
result_depth = open('../result/'+username+'/depth.pkl', 'wb')
def main():
    previous_frame_num = -1
    prev_center = np.zeros((5,3))
    while True:
        frame_num = v3dfbf.get_frame_num()
        if(v3dfbf.frame_from > frame_num):
            v3dfbf.streaming(None, None)
            continue
        if not previous_frame_num == frame_num:
            previous_frame_num = frame_num
            fish_img, rs_img = v3dfbf.get_images()
            timestamp = datetime.strptime(str(time[frame_num + opti_offset]), '%Y-%m-%d %H:%M:%S.%f')
            opti_obj = eo.get_phone_at(timestamp)
            ec_obj = ec.EgoCoord(fish_img, rs_img, verts[frame_num + rs_offset], grav[frame_num])
            if ec_obj.valid:
                # prev_center = ec_obj.prev_center
                xunit, yunit, zunit = ec_obj.xunit_phone, ec_obj.yunit_phone, ec_obj.zunit_phone
                axis = np.array([xunit,yunit,zunit]).reshape(9)
                xunit_, yunit_, zunit_ = opti_obj.x_axis, opti_obj.y_axis, opti_obj.z_axis
                axis_ = np.array([xunit_,yunit_,zunit_]).reshape(9)
                global result_gt
                result_gt = np.concatenate((result_gt,[opti_obj.center]))
                if output_type[0]:
                    global result_p, result_p_axis, result_gt_axis
                    result_p = np.concatenate((result_p,[ec_obj.center_phone]))
                    result_p_axis = np.concatenate((result_p_axis,[axis]))
                    result_gt_axis = np.concatenate((result_gt_axis,[axis_]))
                elif output_type[1]: 
                    global result_input             
                    result_input = np.concatenate((result_input,[
                                                    [ec_obj.RShoulder3D[0],ec_obj.RShoulder3D[1],ec_obj.RShoulder3D[2],
                                                    ec_obj.LShoulder3D[0],ec_obj.LShoulder3D[1],ec_obj.LShoulder3D[2],
                                                    ec_obj.Neck3D[0],ec_obj.Neck3D[1],ec_obj.Neck3D[2],
                                                    ec_obj.grav[0],ec_obj.grav[1],ec_obj.grav[2]
                                                    ]]))
                elif output_type[2]:
                    global bodypoint2D
                    bodypoint2D = np.concatenate((bodypoint2D,[
                                                    [ec_obj.RShoulder2DonDepth.x,ec_obj.RShoulder2DonDepth.y,
                                                    ec_obj.LShoulder2DonDepth.x,ec_obj.LShoulder2DonDepth.y,
                                                    ec_obj.Neck2DonDepth.x,ec_obj.Neck2DonDepth.y]]))
                    v = verts[frame_num + rs_offset]
                    depth = np.sqrt(v[:,:,0]*v[:,:,0]+v[:,:,1]*v[:,:,1]+v[:,:,2]*v[:,:,2])
                    depth[depth>max_depth] = max_depth
                    depth  = depth/max_depth
                    np.save(result_depth, depth)
                out_fisheye.write(ec_obj.equirectangular_img)
                out_rs_RGB.write(ec_obj.rs_img)
                print("frame:",frame_num)
                print("truth:",opti_obj.center)
                print("predict:",ec_obj.center_phone)
                print("--------------------")
        v3dfbf.streaming(opti_obj, ec_obj)
        cv2.imshow('fisheye cam', ec_obj.equirectangular_img)
        cv2.imshow('realsense cam', ec_obj.rs_img)
        if(frame_num == v3dfbf.frame_to-1):
            print('Task complete')
            break
        if cv2.waitKey(1) == 27:
            print("User pressed exit button")
            break

    np.savetxt("../result/"+username+"/groundtruth.csv",result_gt,delimiter=',')
    if output_type[0]:
        np.savetxt("../result/"+username+"/prediction.csv",result_p,delimiter=',')
        np.savetxt("../result/"+username+"/prediction_axis.csv",result_p_axis,delimiter=',')
        np.savetxt("../result/"+username+"/groundtruth_axis.csv",result_gt_axis,delimiter=',')
    elif output_type[1]:
        np.savetxt("../result/"+username+"/input.csv",result_input,delimiter=',')
    elif output_type[2]:
        np.savetxt("../result/"+username+"/bodypoint2D.csv",bodypoint2D,delimiter=',')
        result_depth.close()
    print("Finished!")

if __name__ == '__main__':
    main()