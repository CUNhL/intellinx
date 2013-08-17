package com.intellinx.us.ps.model.common;

import java.util.Date;

import com.intellinx.bom.entity.Principal;

/**
 * 
 * @author RenatoM
 * 
 */
public abstract interface IModifyInfo {
	
	public abstract void setLastModified(Date paramDate);

	public abstract void setLastModifiedBy(Principal paramPrincipal);
	
}
