
public class Transition {
	public State _from;
	public State _to;
	public String _guard;
	public String _assignment;
	
	public Transition(State from, State to, String guard, String assignment){
		_from = from;
		_to = to;
		_guard = guard;
		_assignment = assignment;
	}
	
	public Transition(){
		_from = new State();
		_to = new State();
		_guard = "none";
		_assignment = "none";
	}

	@Override
	public String toString() {
		return "Transition [_from=" + _from + ", _to=" + _to + ", _guard=" + _guard + ", _assignment=" + _assignment
				+ "]";
	}
	

}
