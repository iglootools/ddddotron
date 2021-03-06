CREATE TABLE event(
    id BIGINT NOT NULL IDENTITY,
-- unit of work - commit
    stream_type VARCHAR(200) NOT NULL,
    stream_id VARCHAR(200) NOT NULL,
    revision BIGINT NOT NULL,
-- event payload
    payload_type VARCHAR(200) NOT NULL,
    payload_version SMALLINT NOT NULL,
-- json data representing the event
    payload CLOB NOT NULL,
-- whether the event has been dispatched (to avoid 2PC)
    dispatched BOOLEAN DEFAULT FALSE NOT NULL,
-- to support replaying events by date
    event_timestamp TIMESTAMP NOT NULL,
    CONSTRAINT event_natural_pk UNIQUE (stream_type, stream_id, revision)
);

CREATE INDEX event_dispatched_idx ON event(dispatched);
CREATE INDEX event_timestamp_idx ON event(event_timestamp);


CREATE TABLE stream_snapshot(
    id BIGINT NOT NULL IDENTITY,
-- metadata to determine stream to which this event applies (type,id)
    stream_type VARCHAR(200) NOT NULL,
    stream_id VARCHAR(200) NOT NULL,
-- snapshot data
    includes_commits_up_to_revision BIGINT NOT NULL,
    snapshot_payload CLOB NOT NULL,

    CONSTRAINT stream_natural_pk UNIQUE (stream_type, stream_id)
);