/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Brissy Maxence & Duplessis Vincent
 */
public class State {
    public final String _id;
    public final String _name;
	
    public State(String id, String name){
	_id = id;
	_name = name;
    }
	
    public State(){
	_id = "none";
	_name = "none";
    }
	
    @Override
    public String toString(){
	return _name;
    }    
}
