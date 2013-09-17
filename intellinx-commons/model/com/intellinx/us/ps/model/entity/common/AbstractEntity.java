package com.intellinx.us.ps.model.entity.common;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.intellinx.bom.annotations.Alias;
import com.intellinx.bom.annotations.CreateInfo;
import com.intellinx.bom.annotations.DisplayName;
import com.intellinx.bom.annotations.ModifyInfo;
import com.intellinx.bom.annotations.ReadOnly;
import com.intellinx.bom.entity.Principal;
import com.intellinx.us.ps.model.common.ICreateInfo;
import com.intellinx.us.ps.model.common.IId;
import com.intellinx.us.ps.model.common.IModifyInfo;

/**
 * 
 * @author Renato Mendes
 * 
 */
@MappedSuperclass
public class AbstractEntity implements Serializable, ICreateInfo, IModifyInfo,
		IId {
	private static final long serialVersionUID = -7572476490520397564L;

	@Id
	@GeneratedValue(generator = "ENTITY_GENERATOR", strategy = GenerationType.TABLE)
	@TableGenerator(name = "ENTITY_GENERATOR", allocationSize = 1000, table = "COMMON_SEQ_ENTITY")
	@Column(name = "ID")
	@DisplayName(name = "Id")
	private Long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CREATED")
	@DisplayName(name = "Created")
	@Alias("created")
	@ReadOnly
	@CreateInfo
	private Date created;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "CREATED_BY_FK")
	@DisplayName(name = "Created By")
	@Alias("created_by")
	@ReadOnly
	@CreateInfo
	private Principal createdBy;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "LAST_MODIFIED")
	@DisplayName(name = "Last Modified")
	@Alias("last_modified")
	@ReadOnly
	@ModifyInfo
	private Date lastModified;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "LAST_MODIFIED_BY_FK")
	@DisplayName(name = "Last Modified By")
	@Alias("last_modified_by")
	@ReadOnly
	@ModifyInfo
	private Principal lastModifiedBy;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreated() {
		return this.created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Principal getCreatedBy() {
		return this.createdBy;
	}

	public void setCreatedBy(Principal createdBy) {
		this.createdBy = createdBy;
	}

	public Date getLastModified() {
		return this.lastModified;
	}

	public void setLastModified(Date lastModified) {
		/* 104 */this.lastModified = lastModified;
	}

	public Principal getLastModifiedBy() {
		/* 108 */return this.lastModifiedBy;
	}

	public void setLastModifiedBy(Principal lastModifiedBy) {
		/* 112 */this.lastModifiedBy = lastModifiedBy;
	}

}
