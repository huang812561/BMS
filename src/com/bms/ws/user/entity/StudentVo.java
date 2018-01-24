package com.bms.ws.user.entity;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class StudentVo implements Serializable {
	 /**
     * 
     */
    private static final long serialVersionUID = -5481609827444603990L;
    
    private Integer id;
    private String name;
    private Integer age;
    private String desc;
    private Boolean status;

    public StudentVo() {
    }

    public StudentVo(Integer id, String name, Integer age, String desc,
            Boolean status) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.desc = desc;
        this.status = status;
    }

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	@Override
    public String toString() {
        return "{id=" + id + ", name=" + name + ", age=" + age + ", desc="
                + desc + ", status=" + status + "}";
    }
}
