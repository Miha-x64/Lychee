package net.aquadc.properties.sql

import net.aquadc.persistence.type.long
import net.aquadc.properties.testing.SomeSchema


val SomeTable = SimpleTable<SomeSchema, Long>(SomeSchema, "some_table", long, "_id")
