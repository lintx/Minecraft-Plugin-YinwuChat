/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lintx.plugins.yinwuchat.bungee.json;

/**
 *
 * @author LinTx
 */
public class InputMessage extends InputBase {
    private final String message;
    
    public String getMessage(){
        return message;
    }
    
    InputMessage(String message){
        if (message == null) {
            message = "";
        }
        this.message = message;
    }
}
