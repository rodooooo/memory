public class Instruction 
{
  public String inst;
  public long addr;
  public long endAddr = -1; // NUEVO: fin del rango

  public Instruction( String inst, long addr ) 
  {
    this.inst = inst;
    this.addr = addr;
  } 	
  // NUEVO: constructor para rangos
  public Instruction( String inst, long addr, long endAddr ) 
  {
    this.inst = inst;
    this.addr = addr;
    this.endAddr = endAddr;
  }
  // NUEVO: saber si es rango
  public boolean isRange() {
    return endAddr != -1;
  }
}