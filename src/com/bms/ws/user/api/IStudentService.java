package com.bms.ws.user.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bms.ws.user.entity.StudentListVo;
import com.bms.ws.user.entity.StudentVo;
 
@Path("/studentService")
@Produces(MediaType.APPLICATION_JSON)
public interface IStudentService {
 
    @GET
    @Path("/status")
    public String getStatus();
 
    @GET
    @Path("/students/{index}")
    public StudentVo getStudentById(@PathParam("index") Integer id);
 
    @GET
    @Path("/students")
    public StudentListVo getStudentList();
}