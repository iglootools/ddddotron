#!/usr/bin/env python
from migrate.versioning.shell import main
main(url='postgres://ddddotron:ddddotron@localhost/ddddotron_test', debug='False', repository='eventstore')
