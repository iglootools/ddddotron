from sqlalchemy import *
from migrate import *

meta = MetaData()
event = Table('event', meta,
    Column('id', BigInteger, primary_key=True),
    Column('stream_type', String(200), nullable=False),
    Column('stream_id', String(200), nullable=False),
    Column('revision', BigInteger(), nullable=False),
    Column('payload_type', String(200), nullable=False),
    Column('payload_version', SmallInteger(), nullable=False),
    Column('payload', Text(), nullable=False),
    Column('dispatched', Boolean(), nullable=False, server_default='false'),
    Column('event_timestamp', DateTime(), nullable=False),
    UniqueConstraint('stream_type', 'stream_id', 'revision', name='event_natural_pk')
)

stream_snapshot = Table('stream_snapshot', meta,
    Column('id', BigInteger, primary_key=True),
    Column('stream_type', String(200), nullable=False),
    Column('stream_id', String(200), nullable=False),
    Column('includes_commits_up_to_revision', BigInteger(), nullable=False),
    Column('snapshot_payload', Text(), nullable=False),
    UniqueConstraint('stream_type', 'stream_id', name='stream_natural_pk')
)

def upgrade(migrate_engine):
    # Upgrade operations go here. Don't create your own engine; bind migrate_engine
    # to your metadata
    meta.bind = migrate_engine
    event.create()
    stream_snapshot.create()

def downgrade(migrate_engine):
    # Operations to reverse the above upgrade go here.
    meta.bind = migrate_engine
    event.drop()
    stream_snapshot.drop()
