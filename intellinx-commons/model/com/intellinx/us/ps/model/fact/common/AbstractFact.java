package com.intellinx.us.ps.model.fact.common;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.intellinx.bom.annotations.DisplayFormat;
import com.intellinx.bom.annotations.DisplayName;
import com.intellinx.us.ps.model.common.IId;
import com.intellinx.us.ps.model.event.common.AbstractEvent;

/**
 * 
 * @author RenatoM
 * 
 */
@MappedSuperclass
public abstract class AbstractFact implements Serializable, IId {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5879960368596482292L;

	@Id
	@GeneratedValue(generator = "FACT_GENERATOR", strategy = GenerationType.TABLE)
	@TableGenerator(name = "FACT_GENERATOR", allocationSize = 1000, table = "COMMON_SEQ_FACT")
	@Column(name = "ID")
	@DisplayName(name = "Id")
	private Long id;

	@Temporal(TemporalType.TIMESTAMP)
	@DisplayName(name = "Timestamp")
	@Column(name = "TIMESTAMP_F")
	@DisplayFormat(format = "MM/dd/yyyy HH:mm:ss:S")
	private Date timestamp;

	@ManyToMany(fetch = FetchType.LAZY)
	private List<AbstractEvent> events;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<AbstractEvent> getEvents() {
		return events;
	}

	public void setEvents(List<AbstractEvent> events) {
		this.events = events;
	}

}
