QT -= gui
QT += core

TEMPLATE = app
CONFIG += console
CONFIG -= app_bundle


include (../talk/talk_common.pri)

DESTDIR = $$output_dir

INCLUDEPATH     +=  \
    ../third_party/jsoncpp/overrides/include ../third_party/jsoncpp/source/include \


win32 {

LIBS +=-lwinmm -liphlpapi -lcomsupp -lsecur32 -lws2_32  -lcrypt32 -lAdvapi32 -luser32
LIBS += -lStrmiids -lshell32

    LIBS += -L$$output_dir/libs
    LIBS +=   -ljingle_app  -ljingle_p2p  -ljingle
    LIBS += -ljsoncpp



} else {
    #QMAKE_CXXFLAGS += -std=c++11

    LIBS += -L$$output_dir/libs



    LIBS +=   -ljingle_app  -ljingle_p2p  -ljingle
    LIBS += -ljsoncpp

    LIBS += -pthread -ldl

    #PRE_TARGETDEPS += $$DESTDIR/libjsoncpp.a $$DESTDIR/libjingle_p2p.a $$DESTDIR/libjingle.a $$DESTDIR/libjingle_app.a

}





SOURCES += \
    main.cpp \
    peer_connection_client.cc \
    defaults.cc \
    ServerConductor.cpp \
    p2pthread.cpp \
    streamprocess.cpp \
    peertunnel.cpp

HEADERS += \
    peer_connection_client.h \
    defaults.h \
    ServerConductor.h \
    p2pthread.h \
    streamprocess.h \
    peertunnel.h

