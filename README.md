# ꩜ Nautilus for Roam

Nautilus is a Roam Research extension designed for practical, stress-free daily task planning. It presents tasks and calendar events in a watch-face-like spiral within the Roam interface, acknowledging firmly that tasks vary in duration. 

Traditional time-boxing solutions are stressful due to rigid schedules; if one fails to keep up, the agenda quickly derails, which causes demotivation. Nautilus is more flexible, dynamically repositioning unfinished tasks within your day's remaining open time slots. This actually *reduces* stress (try it:), improves prioritization and task planning skills, and mercifully clarifies feasible tasks at any given moment.

Why spiral shape? In my experience, my energy available for doing creative and demanding tasks slowly diminishes during a day – and the shape reflects it. 

<img src="https://github.com/tombarys/roam-depot-nautilus/raw/main/examplewithlegend.png" width="800"></img>

_Events are yellow, tasks are blue – and done tasks are faint gray_


## General instructions

- Tasks and events are just rows of simple **text outline** placed as a children’s block under the Nautilus component. You can use easy "caldown" notation too. Nautilus dynamically visualizes the outline into a watch-like visual timeline. It works in real time.
- The order and the length of **tasks are sacred** – only you can manually change it.  
- A notable and the only "automatic" feature of Nautilus is task **"push-forward," relocating uncompleted tasks** within open time slots in your day – taking the present time into account. The spiral reflects time without your intervention.
- If the component is **not placed into today’s Daily Page**, the red time beam is not shown, and tasks are not pushed into the future. (As it does not make sense, you are reviewing the day’s agenda or planning).
- You can define your agenda using a straightforward notation:
  - **events** are rows containing a *time range* in 24h format (`HH:MM-HH:MM`, minutes can be omitted) and are fixed until the time range is changed by the user (e.g., "12:30-13 Meeting with JK", "9-10 Breakfast"). 
  - **tasks** are all residuing rows that are not events; undone tasks move through a day (e.g., "nearly empty task" "{{[[TODO]]}} another, but important task").
- **Tasks duration defaults to a 15-minute time allocation**, but this can be adjusted above in Roam Depot extension settings or **individually for each task in your task list** with the simple notation `Mm` or `Mmin` where M is the length of a task in minutes (e.g., "Call Jack 10m", "Daily workout 45min").
- The **order of tasks** in Nautilus spiral **reflects exactly the order of tasks in the list.** 
- Tasks can be **forced to follow after a particular event** too – simply by placing them after the event in the task list; for example: to plan "Nap" not before lunch, just put it after it in the list. This does not change the order of tasks.
- Once a task is marked 'DONE' in Roam, (and if you have installed the [Todo Trigger extension](https://github.com/tombarys/roam-depot-nautilus/blob/main/README.md#1-install-todo-trigger-extension-for-better-experience)) it's **tagged with the completion time** in format dHH:MM (e.g. d14:30), which is visually interpreted as a faint grey section on the spiral. It can serve as a visual log of tasks that have been done.


### Additional info + tips and tricks
- Nautilus works pretty well on mobile too.
- I suggest describing your tasks in a very short (BuJo-like) style. Add detailed description into children blocks of task.
- References and markdown links are stripped heavily to show only the real name of the task in legend. 
- You can use "to" in events time range definiton too (e.g., "14:30 to 15:15 My TED Talk"). 
- You can use Roam references to blocks in your task list. It means you can just `Alt/Option` + drag and drop tasks from other pages/blocks without rewriting them from scratch.
- Do your planning in the morning or even the evening before. It seems like Nautilus involves extended preparation before a  workday, but my experience is it greatly aids conscious day planning and eventual task optimization. 
- I usually add a simple `#Today` tag at the start of the Nautilus render block too – it helps when you want to enter the *click to edit* mode in Roam. For example: `#Today {{[[roam/render]]:((roam-render-Nautilus-cljs)) 22 30}}`.
- Sorry for some glitches when generating the proper position of the legend in some edge cases. This is MVP – even I am using Nautilis for 6 months without bigger issues, there are still some problems and lot of work has to be done. 


## Setup (you do this once)
### 1. First, make sure that __User code__ is enabled in your settings. 
This allows custom components in your graph. 

<img src="https://github.com/8bitgentleman/roam-depot-tidy-todos/raw/main/settings.png" width="400"></img>

Technically, the spiral component is a code inserted using a Roam template into a block on a Daily Page. 

### 2. Install the TODO Trigger extension for a better experience
I strongly suggest installing great [David Vargas](https://github.com/dvargas92495/roamjs-todo-trigger)'s **Todo Trigger extension** from Roam Depot before using Nautilus and setting it to add a timestamp when todo is done automatically. 

<img src="https://github.com/tombarys/roam-depot-nautilus/blob/31e8113651badce77da0eabac5d4a6e4fa657b60/todotrigger.png?raw=true" width="400"></img>


### 3. Adjust your settings

Additionally, you can easily change two parameters to better suit your needs (in Roam Depot extension Settings):
- the length of the legend text (longer task description than specified will be stripped from spiral legend)
- the default duration of the task (when creating a new todo, you can leave it without specifics and it will default to the setting)

Important: All settings will not manifest retroactivelly in old Nautiluses, but just when creating a new instance using the `;;` Nautilus template. 

## Daily use (you do this every day)
### 1. Insert the component into your Daily Page
The easiest way to insert the component is through Roam's native template menu. Type `;;` and look for "Nautilus." Press `Enter`.

<img src="https://github.com/tombarys/roam-depot-nautilus/raw/main/template1.png" width="500"></img>

which inserts this code:

<img src="https://github.com/tombarys/roam-depot-nautilus/raw/main/template2.png" width="500"></img>

Press `Enter` and `Tab`. Now you can start writing your task list.

### 3. Put your tasks and events into the children’s block 
- If you are not able to edit the children of the Nautilus block, try pressing `Cmd(Ctrl)-Enter` which opens the first block of the page for editing. 
- Move or indent a list of your todos into a children of the block of the Nautilus.
- From now you can edit and rearrange children blocks as you wish and see how your work spots are dynamically rearranged and filled with your tasks. 

Enjoy!

## Credits

- Huge thanks to Roam Slack community, especially to Matt Vogel, which helped me to understand how the Roam Depot extensions (roam/render) work. His [Roam Depot Render Template](https://github.com/8bitgentleman/roam-depot-render-template) is being adopted here. Many thanks to [Adam Kalisz](https://x.com/kaliszad), a developer behind OrgPad (written in ClojureScript) that helped me to grasp functional programming. Thanks to my beta-testers too.

