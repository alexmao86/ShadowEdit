package shadowedit;

import org.eclipse.ui.IStartup;
/**
 * 触发activator 加载. 插件延迟加载机制会在第一个插件class加载时开始加载插件
 * @author anping
 *
 */
public class Starter implements IStartup{
	
	@Override
	public void earlyStartup() {
		Activator.init();
		System.out.println("starter called");
	}

}
