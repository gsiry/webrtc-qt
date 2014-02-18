#-------------------------------------------------
#
# Project created by QtCreator 2014-02-15T09:41:57
#
#-------------------------------------------------

QT       -= core gui

TARGET = jingle_p2p
TEMPLATE = lib
CONFIG += staticlib

CONFIG -= app_bundle
CONFIG -= qt

DESTDIR = $$PWD/../bin

INCLUDEPATH += ..



win32 {

Debug:DEFINES +=_DEBUG

Release:DEFINES +=

DEFINES += _UNICODE UNICODE


} else:mac {
} else {
    DEFINES += POSIX LOGGING=1
}

SOURCES += \
    ../talk/p2p/base/constants.cc \
    ../talk/p2p/base/basicpacketsocketfactory.cc \
    ../talk/p2p/base/port.cc \
    ../talk/p2p/base/stun.cc \
    ../talk/p2p/base/stunport.cc \
    ../talk/p2p/base/stunrequest.cc \
    ../talk/p2p/base/stunserver.cc \
    ../talk/p2p/base/asyncstuntcpsocket.cc \
    ../talk/p2p/base/portallocator.cc \
    ../talk/p2p/base/relayserver.cc \
    ../talk/p2p/base/p2ptransportchannel.cc \
    ../talk/p2p/base/portallocatorsessionproxy.cc \
    ../talk/p2p/base/tcpport.cc \
    ../talk/p2p/base/relayport.cc \
    ../talk/p2p/base/turnport.cc \
    ../talk/p2p/base/transportchannel.cc \
    ../talk/p2p/base/portproxy.cc \
    ../talk/p2p/base/pseudotcp.cc \
    ../talk/p2p/base/transportchannelproxy.cc \
    ../talk/p2p/base/transportdescription.cc \
    ../talk/p2p/base/transport.cc \
    ../talk/p2p/base/p2ptransport.cc \
    ../talk/p2p/base/session.cc \
    ../talk/p2p/base/sessiondescription.cc \
    ../talk/p2p/base/dtlstransportchannel.cc \
    ../talk/p2p/base/transportdescriptionfactory.cc \
    ../talk/p2p/client/basicportallocator.cc \



HEADERS += \
    ../talk/p2p/base/basicpacketsocketfactory.h \
    ../talk/p2p/base/stun.h \
    ../talk/p2p/base/stunport.h \
    ../talk/p2p/base/stunrequest.h \
    ../talk/p2p/base/stunserver.h \
    ../talk/p2p/base/port.h \
    ../talk/p2p/base/asyncstuntcpsocket.h \
    ../talk/p2p/base/portallocator.h \
    ../talk/p2p/client/basicportallocator.h \
    ../talk/p2p/base/relayserver.h \
    ../talk/p2p/base/p2ptransportchannel.h \
    ../talk/p2p/base/portallocatorsessionproxy.h \
    ../talk/p2p/base/tcpport.h \
    ../talk/p2p/base/relayport.h \
    ../talk/p2p/base/turnport.h \
    ../talk/p2p/base/transportchannel.h \
    ../talk/p2p/base/transportchannelimpl.h \
    ../talk/p2p/base/portproxy.h \
    ../talk/p2p/base/pseudotcp.h \
    ../talk/p2p/base/transportchannelproxy.h \
    ../talk/p2p/base/transportdescription.h \
    ../talk/p2p/base/transport.h \
    ../talk/p2p/base/p2ptransport.h \
    ../talk/p2p/base/session.h \
    ../talk/p2p/base/constants.h \
    ../talk/p2p/base/sessiondescription.h \
    ../talk/p2p/base/dtlstransport.h \
    ../talk/p2p/base/dtlstransportchannel.h \
    ../talk/p2p/base/transportdescriptionfactory.h \

