package com.bms.ws.user.impl;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.springframework.beans.factory.annotation.Autowired;

import com.bms.ws.user.api.IStudentService;
import com.bms.ws.user.entity.StudentDateSet;
import com.bms.ws.user.entity.StudentListVo;
import com.bms.ws.user.entity.StudentVo;

@Path("/studentService")
public class StudentServiceImpl implements IStudentService {
    @Autowired
    private StudentDateSet studentDateSet;

    /**
     * url:http://<host>:<port>/<appcontext>/services/studentService/status
     */
    @Override
    @GET
    @Path("/status")
    public String getStatus() {
    	 System.out.println("status");  
        return "getStatus";
    }

    /**
     * url:http://<host>:<port>/<appcontext>/services/studentService/students/{
     * index}
     */
    @Override
    @GET
    @Path("/students/{index}")
    public StudentVo getStudentById(@PathParam("index") Integer id) {
        List<StudentVo> studentList = StudentDateSet.getList();
        StudentVo vo = null;
        if (studentList.size() > id) {
            vo = studentList.get(id - 1);
        }
        return vo;
    }

    /**
     * url:http://<host>:<port>/<appcontext>/services/studentService/students
     */
    @Override
    @GET
    @Path("/students")
    public StudentListVo getStudentList() {
        List<StudentVo> studentList = StudentDateSet.getList();
        StudentListVo listVo = new StudentListVo(studentList);
        return listVo;
    }

}