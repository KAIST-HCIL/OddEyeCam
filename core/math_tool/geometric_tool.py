import numpy as np
from skimage.measure import LineModelND, ransac
import scipy.linalg
from core.math_tool.coordinate_system import CoordSys

class GeoTool():

    def unitvec(self,u):
        return u/np.sqrt(np.dot(u,u))

    def get_angle(self, u,v,sign_idx):
        uv_dot = u[:,0]*v[:,0]+u[:,1]*v[:,1]+u[:,2]*v[:,2]
        uu_dot = u[:,0]*u[:,0]+u[:,1]*u[:,1]+u[:,2]*u[:,2]
        vv_dot = v[:,0]*v[:,0]+v[:,1]*v[:,1]+v[:,2]*v[:,2]
        angle = np.degrees(np.arccos(uv_dot/np.sqrt(uu_dot*vv_dot)))
        angle[u[:,sign_idx] < 0] = -angle[u[:,sign_idx] < 0]
        return angle

    def angleNaxis(self,u,v):
        u = u/(np.sqrt(np.dot(u[0],u[0])+np.dot(u[1],u[1])+np.dot(u[2],u[2])))
        v = v/(np.sqrt(np.dot(v[0],v[0])+np.dot(v[1],v[1])+np.dot(v[2],v[2])))
        axis = np.cross(v,u)
        angle = np.arccos(np.dot(u,v))
        angle = np.degrees(angle)
        axis = self.unitvec(axis)
        return angle, axis

    # def rotation(self, axis, theta, v):
    #     """ 
    #     Return the rotation matrix associated with counterclockwise rotation about
    #     the given axis by theta radians.
    #     """
    #     theta = np.radians(theta)
    #     axis = np.asarray(axis)
    #     axis = self.unitvec(axis)
    #     a = np.cos(theta / 2.0)
    #     b, c, d = -axis * np.sin(theta /2.0)
    #     aa, bb, cc, dd = a * a, b * b, c * c, d * d
    #     bc, ad, ac, ab, bd, cd = b * c, a * d, a * c, a * b, b * d, c * d
    #     rotM =  np.array([[aa + bb - cc - dd, 2 * (bc + ad), 2 * (bd - ac)],
    #                      [2 * (bc - ad), aa + cc - bb - dd, 2 * (cd + ab)],
    #                      [2 * (bd + ac), 2 * (cd - ab), aa + dd - bb - cc]])
    #     result = np.transpose(np.dot(rotM, np.transpose(v)))
    #     result = result * np.linalg.norm(v) / np.linalg.norm(result)
    #     return result

    def rotation(self, axis, theta, v):
        theta = np.radians(theta)
        axis = self.unitvec(axis)
        wx,wy,wz = axis[0],axis[1],axis[2]
        e11 = np.cos(theta)+(wx**2)*(1-np.cos(theta))
        e12 = wx*wy*(1-np.cos(theta)) - wz*np.sin(theta)
        e13 = wy*np.sin(theta)+wx*wz*(1-np.cos(theta))
        e21 = wz*np.sin(theta) + wx*wy*(1-np.cos(theta))
        e22 = np.cos(theta) + (wy**2)*(1-np.cos(theta))
        e23 = -wx*np.sin(theta) + wy * wz*(1-np.cos(theta))
        e31 = -wy*np.sin(theta) + wx*wz*(1-np.cos(theta))
        e32 = wx*np.sin(theta) + wy*wz*(1-np.cos(theta))
        e33 = np.cos(theta) + (wz**2)*(1-np.cos(theta))
        rotM = np.array([[e11,e12,e13],
                         [e21,e22,e23],
                         [e31,e32,e33]])
        v = np.expand_dims(v, 1)
        result = np.matmul(rotM,v)
        result = result * np.linalg.norm(v) / np.linalg.norm(result)
        return result.flatten()

    def get_points_on_2D_line(self,point1,point2):
        x1, y1 = point1[1],point1[0]
        x2, y2 = point2[1],point2[0]
        a = (y1-y2)/(x1-x2)
        b = (y1*x2 - y2*x1)/(x2-x1)
        x = np.arange(x1,x2).astype(np.int)
        y = np.around((a*x+b)).astype(np.int)
        return np.array([y,x]).transpose()

    def linear_regression(self, v, threshold = 500):
        if v.shape[0] < 2:
            return np.array([1,0,0])
        model_robust, inliers = ransac(v, LineModelND, min_samples=2,
                               residual_threshold=threshold, max_trials=100)
        outliers = inliers == False
        v_inlier = v[inliers]
        datamean = v_inlier.mean(axis=0)
        uu, dd, vv = np.linalg.svd(v_inlier - datamean)
        linepts = vv[0] * np.mgrid[-7:7:2j][:, np.newaxis]
        linepts += datamean
        vec = linepts[0,:] - linepts[1,:]
        vec = self.unitvec(vec)
        return vec

    def vec2frame(self,coordsys_b,vec,coordsys_a,is_point=True):
        """
        Return new vector
        The input vector is in coordinate system 'b' (from)
        and the new vector is expressed in coordinate system 'a' (to)
        """
        p, q, r = vec[0],vec[1],vec[2]
        cb, ca = coordsys_b.get_center(), coordsys_a.get_center()
        xa, ya, za = coordsys_a.get_x_axis(), coordsys_a.get_y_axis(), coordsys_a.get_z_axis()
        xb, yb, zb = coordsys_b.get_x_axis(), coordsys_b.get_y_axis(), coordsys_b.get_z_axis()
        a = p*np.dot(xb,xa)+q*np.dot(yb,xa)+r*np.dot(zb,xa)
        b = p*np.dot(xb,ya)+q*np.dot(yb,ya)+r*np.dot(zb,ya)
        c = p*np.dot(xb,za)+q*np.dot(yb,za)+r*np.dot(zb,za)
        o = np.array([0,0,0])
        if is_point:
            globalsys = CoordSys(cb,np.array([1,0,0]),np.array([0,1,0]),np.array([0,0,1]))
            o = self.vec2frame(globalsys,cb-ca,coordsys_a,False)
        vec_out = np.array([a,b,c]) + o
        return vec_out

    def get_view_from(self, coordsys, view_to):
        ref = coordsys.ref_coordsys
        center = coordsys.center
        center = self.vec2frame(ref,center,view_to)
        x_axis = coordsys.x_axis
        x_axis = self.vec2frame(ref,x_axis,view_to,False)
        y_axis = coordsys.y_axis
        y_axis = self.vec2frame(ref,y_axis,view_to,False)
        z_axis = coordsys.z_axis
        z_axis = self.vec2frame(ref,z_axis,view_to,False)
        x_axis = self.unitvec(x_axis)
        y_axis = self.unitvec(y_axis)
        z_axis = self.unitvec(z_axis)
        return CoordSys(center, x_axis, y_axis, z_axis, view_to, coordsys.timestamp)
    

    def get_polar_expression_of_chest(self,coordsys):
        vec = coordsys.center
        if(vec.ndim == 1):
            v = np.array([vec])
        x, y, z = v[:,0], v[:,1], v[:,2]
        # d
        d = np.sqrt(x*x+y*y+z*z).astype(np.int)
        # theta
        zero = np.zeros(x.shape[0])
        v_xz = np.concatenate((np.concatenate((x,zero)),z)).reshape(3,-1).transpose()
        v_001 = np.repeat(np.array([[0,0,1]]),x.shape[0],axis=0)
        theta = self.get_angle(v_xz,v_001,0)
        # phi
        phi = -self.get_angle(v,v_xz,1)
        # polar coordinate
        polar_coord = np.transpose(np.array([d, theta, phi]))
        if(vec.ndim == 1):
            polar_coord = polar_coord[0]
        coordsys.set_polar_expression(polar_coord)