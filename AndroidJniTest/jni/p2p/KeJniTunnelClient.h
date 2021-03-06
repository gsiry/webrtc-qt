/*
 * KeJniTunnelClient.h
 *
 *  Created on: 2014-4-1
 *      Author: lht
 */

#ifndef KEJNITUNNELCLIENT_H_
#define KEJNITUNNELCLIENT_H_
//#include "JniUtil.h"

#include "libjingle_app/KeMsgProcessContainer.h"

class KeJniTunnelClient :public KeTunnelClient{
public:
	KeJniTunnelClient();
	virtual ~KeJniTunnelClient();
    virtual void OnTunnelOpened(PeerTerminalInterface * t,const std::string & peer_id);

protected:
    virtual void OnRecvAudioData(const std::string & peer_id,const char * data,int len);
    virtual void OnRecvVideoData(const std::string & peer_id,const char * data,int len);
private:

    //talk_base::Thread *jni_thread;
};

#endif /* KEJNITUNNELCLIENT_H_ */
