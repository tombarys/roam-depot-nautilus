# ꩜ Nautilus for Roam

Nautilus is a Roam Research extension for stress-free task planning, visually representing tasks and calendar events in the Roam Daily Page, and recognizing task duration variabilities. This flexible tool uses the present moment as a threshold to dynamically push unfinished tasks into the available time until tonight while keeping them in a user-defined order with user-estimated durations (no AI!).  

This visual approach reduces the feeling of being overwhelmed, enhances task-effort estimating skills, and clearly shows what the feasible tasks are for the rest of the day. The spiral shape mirrors one's diminishing energy for creative tasks over a given day.

<img src="https://github.com/tombarys/roam-depot-nautilus/raw/main/examplewithlegend.png" width="800"></img>

_Events are yellow, tasks are blue – and done tasks are light gray_

## Quick Start Guide

- only once: install **Nautilus** and **TODO Trigger** extension
- insert the component at the top of your Daily Page using `;;Nautilus` template
- indent all events and tasks that you want to accomplish below the component as its children 
- start inserting tasks, then folow up with events
- use e.g. "12:30-14:20" or "9am to 1:45pm" to anchor fixed time events
- use e.g. "10m" or "20min" to set the task duration (other than default)
- use e.g. "d18:30" or "d9:05" in tasks to denote the time when they were finished (or use extension for it)
- during a day: reorder tasks and move some of them after fixed events to ensure they will not be planned earlier


## Quick Start Video

<img src="https://github.com/tombarys/roam-depot-nautilus/blob/e5517dbdf8f873e1e5041219de3da36f376dcfca/videothumb.png?raw=true" width="600"></img>

[Click here to see the video.](https://www.loom.com/embed/c66b99a39a5a4f74b2889ccab467e9eb?sid=5809279e-7deb-44e2-a448-f9de33ba8510)

## Setup (you do this only once)
### 1. First, make sure that __User code__ is enabled in your settings. 
This allows custom components in your graph. 

<img src="https://github.com/8bitgentleman/roam-depot-tidy-todos/raw/main/settings.png" width="400"></img>

Technically, the spiral component is a code inserted using a Roam template into a block on a Daily Page. 

### 2. Install the TODO Trigger extension for a better experience
I strongly suggest installing [David Vargas](https://github.com/dvargas92495/roamjs-todo-trigger)'s great **Todo Trigger extension** from the Roam Depot before using Nautilus and setting it to add a timestamp when to-do is done automatically. 

<img src="https://github.com/tombarys/roam-depot-nautilus/blob/31e8113651badce77da0eabac5d4a6e4fa657b60/todotrigger.png?raw=true" width="400"></img>


### 3. Adjust your settings

Additionally, you can easily change parameters to better suit your needs (in the Roam Depot extension Settings):
- your workday start time; choices are 6am, 7am, 8am (default)
- the text (e.g. tag) prefix that will inserted above the the spiral 
- the length of the legend text (longer task description than specified will be stripped from spiral legend)
- the default duration of the task (when creating a new to-do, you can leave it without specifics and it will default to the setting)

Important: All settings will not manifest retroactively in old Nautiluses, but just when creating a new instance using the `;;Nautilus` template. 

## Daily Use (you do this every day)

### 1. Insert the component into your Daily Page
The easiest way to insert the component is through Roam’s native template menu. Type `;;` and select “Nautilus...”. Press Enter or click on it.

<img src="https://github.com/tombarys/roam-depot-nautilus/raw/main/template1.png" width="500"></img>

which inserts this code:

<img src="https://github.com/tombarys/roam-depot-nautilus/raw/main/template2.png" width="500"></img>

Press `Enter` again and `Tab` to indent the first child block under the component. Now you can start writing your task list.

### 2. Put your tasks and events as a child block 
- If you are not able to edit the children blocks of the Nautilus block, try pressing `Cmd(Ctrl)-Enter` which opens the first block of the page for editing. 
- Move or indent a list of your to-dos into a Nautilus children blocks.
- From now you can edit and rearrange your children blocks as you wish and see how your work spots are dynamically rearranged and filled with your tasks. 

Enjoy!

## Additional Info 
- The order and the duration of your tasks **can be changed only by you**. The app does not change it.  
- A notable and the only "automatic" feature of Nautilus is the task **push-forward: relocating uncompleted tasks** within open time slots in your day – taking the present moment into account. The spiral constantly reflects time without your intervention.
- If the Nautilus is **not placed into _today’s_ Daily Page**, the red time beam is not shown, and tasks are not pushed into the future. 
- You can define your agenda using a straightforward notation:
  - **events** are rows containing a *time range* in 24h or 12h format (`HH:MM-HH:MM`, minutes can be omitted) and are fixed until the time range is changed by the user (e.g., "12:30-13 Meeting with JK", "9-10am Breakfast", "11am-14pm Presentation"). 
  - **tasks** are all residual rows that are not events; undone tasks move through a day (e.g., "nearly empty task" "{{[[TODO]]}} another, but important task").
- **Tasks duration defaults to a 15-minute time allocation**, but this can be adjusted in the Roam Depot extension Settings or **individually for each task in your task list** with the simple notation `Mm` or `Mmin` where M is the length of a task in minutes (e.g., "Call Jack 10m", "Daily workout 45min").
- The **order of tasks** in a Nautilus spiral **reflects exactly the order of tasks in the list.** You have to prioritize them by itself – in the morning and later during a day. 
- Tasks can be **forced to follow after a particular event** too – simply by placing them after the event in the task list; for example: to plan "Nap" not before lunch, just put it after it in the list. This does not change the order of tasks.
- Once a task is marked 'DONE' in Roam, (and if you have installed the [Todo Trigger extension](https://github.com/tombarys/roam-depot-nautilus/blob/main/README.md#1-install-todo-trigger-extension-for-better-experience)) it's **tagged with the completion time** in the format dHH:MM (e.g. d14:30), which is visually interpreted as a light grey section on the spiral. It can serve as a visual log of tasks that have been completed.


### Tips and Tricks
- Nautilus works pretty well on mobile too.
- I suggest describing your tasks in a very short (BuJo-like) style. Optionally add detailed description of each into children blocks.
- References and markdown links are stripped heavily to show only the real name of the task in legend. 
- You can use "to" in events time range definition too (e.g., "14:30 to 15:15 My TED Talk"). 
- You can use Roam references to blocks in your task list. It means you can just `Alt/Option` + drag and drop tasks from other pages/blocks without having to rewrite them from scratch.
- Do your planning in the morning or even the evening before. It seems like Nautilus involves extended preparation before a workday, but my experience is that it greatly aids conscientous day planning and eventual task optimization. 
- I usually add a prefix `#Agenda` preceding the Nautilus render block (via Settings). The inserted render blocks look like this: `#Agenda {{[[roam/render]]:((roam-render-Nautilus-cljs)) 22 30 480}}` so clicking later on #Agenda tag helps me to quickly gather all my old Nautiluses.
- Sorry for some glitches when generating the proper position of the legend in some edge cases. This is the first version – even for someone like me that has been using Nautilus for 6 months without bigger issues, there are still some problems and a lot of work still has to be done. 

## Changelog

- 31/1/2024 – now autodetects 12h format in time-range (supports both 24h and 12h time format – if no am/pm specified in the task, defaults to 24h format)
- 1/2/2024 – added an option to change workday start time (the first select option in Settings now, defaults to 8am) – I think the prettiest proportions has a 8am-Nautilus but I understand some of us are early birds!
- 7/2/2024 – added a custom trigger string to individually rewrite task/event color to red

# Feature Requests, Bugs, and Feedback and Credits
Nautilus is work in progress. I am happy to remove bugs or listen to your feedback! Contact me via https://barys.me.

Huge thanks to the Roam Slack community, especially to Matt Vogel, which helped me to understand how the Roam Depot extensions (roam/render) work. His [Roam Depot Render Template](https://github.com/8bitgentleman/roam-depot-render-template) is being adopted here. Thanks to Baibhav Bista from Roam Research company for his kindness and patience with my beginner's mistakes during the review process. Many thanks to [Adam Kalisz](https://x.com/kaliszad), a developer behind OrgPad (written in ClojureScript) that helped me to grasp and find love in functional programming. Thanks to all my beta-testers too.

