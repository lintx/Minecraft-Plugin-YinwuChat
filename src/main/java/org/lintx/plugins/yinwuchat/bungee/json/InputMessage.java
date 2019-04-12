/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.json;

/**
 *
 * @author jjcbw01
 */
public class InputMessage extends BaseInputJSON{
    private final String message;
    
    public String getMessage(){
        return message;
    }
    
    public InputMessage(String message){
        if (message == null) {
            message = "";
        }
        this.message = message;
    }
}
