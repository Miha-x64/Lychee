package net.aquadc.persistence.sql


/**
 * Represents an active record — a container with some values and properties backed by an RDBMS row.
 */
@Deprecated("Record observability is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
typealias Record<SCH, ID> = Nothing
