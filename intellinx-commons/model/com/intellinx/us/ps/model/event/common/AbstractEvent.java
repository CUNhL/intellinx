package com.intellinx.us.ps.model.event.common;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.intellinx.bom.annotations.DisplayFormat;
import com.intellinx.bom.annotations.DisplayName;
import com.intellinx.bom.attribute.RecordingReferenceType;
import com.intellinx.us.ps.model.common.IId;

/**
 * 
 * @author RenatoM
 * 
 */
@MappedSuperclass
public abstract class AbstractEvent implements Serializable, IId {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5879960368596482292L;

	@Id
	@GeneratedValue(generator = "EVENT_GENERATOR", strategy = GenerationType.TABLE)
	@TableGenerator(name = "EVENT_GENERATOR", allocationSize = 1000, table = "COMMON_SEQ_EVENT")
	@Column(name = "ID")
	@DisplayName(name = "Id")
	private Long id;

	@Temporal(TemporalType.TIMESTAMP)
	@DisplayName(name = "Timestamp")
	@Column(name = "TIMESTAMP_F")
	@DisplayFormat(format = "MM/dd/yyyy HH:mm:ss:S")
	private Date timestamp;

	@DisplayName(name = "Session Id")
	@Column(name = "SESSION_ID")
	@com.intellinx.bom.annotations.RecordingReference(RecordingReferenceType.SESSION_ID)
	private String sessionId;

	@DisplayName(name = "Event Number")
	@Column(name = "EVENT_NUMBER")
	@com.intellinx.bom.annotations.RecordingReference(RecordingReferenceType.STEP_NUMBER)
	private String eventNumber;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getEventNumber() {
		return eventNumber;
	}

	public void setEventNumber(String eventNumber) {
		this.eventNumber = eventNumber;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

}
