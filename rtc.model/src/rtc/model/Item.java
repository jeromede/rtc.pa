package rtc.model;

public abstract class Item implements java.io.Serializable {

	private static final long serialVersionUID = 8450163754687832796L;

	private static int nextId = Integer.MIN_VALUE;
	
	private Integer id;
	private String oldId = null;
	private transient String newId = null;
	
	public Item() {
		this.id = nextId++;
	}
	
	public Item(String oldId) {
		this.id = nextId++;
		this.oldId = new String(oldId);
	}

	public int getId() {
		return this.id;
	}

	public String setOldId(String id) {
		return this.oldId = new String(id);
	}

	public String getOldId() {
		return this.oldId;
	}

	public String setNewId(String id) {
		return this.newId = new String(id);
	}

	public String getNewId() {
		return this.newId;
	}

}
