import numpy as np

class CoordSys():
    def __init__(self, center=None, x_axis=None, y_axis=None, z_axis=None, ref_coordsys = None, timestamp = None):
        self.center = center
        self.x_axis = x_axis
        self.y_axis = y_axis
        self.z_axis = z_axis
        self.ref_coordsys = ref_coordsys
        self.timestamp = timestamp

    def set_center(self, center):
        self.center = center

    def set_x_axis(self, x_axis):
        self.x_axis = x_axis

    def set_y_axis(self, y_axis):
        self.y_axis = y_axis

    def set_z_axis(self, z_axis):
        self.z_axis = z_axis

    def set_ref_coordsys(self, ref_coordsys):
        self.ref_coordsys = ref_coordsys

    def get_center(self):
        return np.array(self.center, copy=True)

    def get_x_axis(self):
        return np.array(self.x_axis, copy=True)

    def get_y_axis(self):
        return np.array(self.y_axis, copy=True)

    def get_z_axis(self):
        return np.array(self.z_axis, copy=True)

    def get_ref_coordsys(self):
        return np.array(self.ref_coordsys, copy=True)

    def set_polar_expression(self,polar_coord):
        self.d = polar_coord[0]
        self.theta = polar_coord[1]
        self.phi = polar_coord[2]

    def get_polar_expression(self):
        return np.array([self.d, self.theta, self.phi])

    def arr_to_str_split_by_comma(self,arr):
        result = ''
        n = arr.shape[0]
        temp = arr[0:n-1]
        for a in temp:
            result = result + str(a) + ','
        result = result + str(arr[-1])
        return result

    def to_string(self):
        center = self.arr_to_str_split_by_comma(self.center)
        polar = self.arr_to_str_split_by_comma(self.get_polar_expression())
        x_axis = self.arr_to_str_split_by_comma(self.x_axis)
        y_axis = self.arr_to_str_split_by_comma(self.y_axis)
        z_axis = self.arr_to_str_split_by_comma(self.z_axis)
        data = str('START')+center+str(",")+polar+str(",")+x_axis+str(",")+y_axis+str(",")+z_axis+str('END')
        return data