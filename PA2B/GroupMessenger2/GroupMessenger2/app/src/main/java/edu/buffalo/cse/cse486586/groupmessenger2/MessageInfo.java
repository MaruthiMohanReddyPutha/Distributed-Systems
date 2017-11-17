package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by marut on 3/25/2017.
 */
/*
a serializable message information holder class
 */
public class MessageInfo implements Serializable, Comparable {
    public int sourcePort;
    public int destPort;
    public int msgHash;
    public String msg;
    public int agreedSequence;
    public boolean isDeliverable;
    public boolean isAgreed;
    public boolean isFailure;

    public MessageInfo(String text, int seq, int src, int dest, boolean isAgreement){
        this.msg = text;
        this.sourcePort = src;
        this.destPort = dest;
        this.agreedSequence = seq;
        this.msgHash = msg.hashCode();
        this.isDeliverable = false;
        this.isAgreed = isAgreement;
        this.isFailure = false;
    }

    @Override
    public int compareTo(Object o) {
        MessageInfo nxtMsg = (MessageInfo) o;
        if(this.agreedSequence == nxtMsg.agreedSequence){
            if(this.isDeliverable && nxtMsg.isDeliverable){
                if(this.sourcePort < nxtMsg.sourcePort){
                    return -1;
                }
            }
            if(!this.isDeliverable && nxtMsg.isDeliverable){
                return 1;
            }
        }else{
            if(this.agreedSequence < nxtMsg.agreedSequence){
                return -1;
            }else{
                return 1;
            }
        }
        return 0;
    }
}
