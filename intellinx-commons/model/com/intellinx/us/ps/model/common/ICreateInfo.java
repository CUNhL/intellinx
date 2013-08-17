package com.intellinx.us.ps.model.common;

import java.util.Date;

import com.intellinx.bom.entity.Principal;

/**
 * 
 * @author RenatoM
 * 
 */
public abstract interface ICreateInfo {

	public abstract Date getCreated();

	public abstract void setCreated(Date paramDate);

	public abstract void setCreatedBy(Principal paramPrincipal);
}
