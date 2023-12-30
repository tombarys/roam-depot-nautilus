# Nautilus for Roam

Nautilus is a Roam Research extension designed for practical, stress-free daily task planning. It presents tasks and calendar events in a watch-face-like spiral within the Roam interface, acknowledging firmly that tasks vary in duration. 

Traditional time-boxing solutions are stressful due to rigid schedules; if one fails to keep up, the agenda quickly derails, which causes demotivation. Nautilus is more flexible, dynamically repositioning unfinished tasks within your day's remaining open time slots. This actually *reduces* stress (try it:), improves prioritization and task planning skills, and mercifully clarifies feasible tasks at any given moment.

<img src="https://github.com/tombarys/roam-depot-nautilus/raw/main/examplewithlegend.png" width="800"></img>

_Events are yellow, tasks are blue – and done tasks are faint gray_


## General instructions

- Tasks and events are just items of **text list** placed as a children’s block under the Nautilus component. You can use simple notation, which Nautilus dynamically visualizes into a watch-like visual timeline. It works in real time.
- A notable feature is the automatic task **"push-forward," relocating uncompleted tasks** within open time slots in your day.
- If the component is **not placed into today’s Daily Page**, the red time beam is not shown, and tasks are not pushed into the future. (As it does not make sense, you are reviewing the day’s agenda or planning).
- You can define your agenda using a straightforward notation:
  - **events** are rows containing a *time range* in 24h format (HH:MM-HH:MM, minutes can be omitted) and are unmovable until the time range is changed by the user (e.g., "12:30-13 Meeting with JK", "9-10 Breakfast")
  - **tasks** are all residuing rows that are not events; undone tasks move through a day (e.g., "nearly empty task" "{{[[TODO]]}} another, but important task").
- Tasks **duration defaults to a 15-minute time allocation**, but this can generally be adjusted above in settings or **manually and individually for each task** with the simple notation "Mm" or "Mmin“ where M is the length of a task in minutes (e.g., "Call Jack 10m", "Daily workout 45min").
- The **order of tasks** in Nautilus spiral **reflects exactly the order of tasks in the list.** 
- Tasks can be only **forced to follow after a particular event** – by placing them after the event in the task list; for example, setting a run post-lunch. This does not change the order of tasks.
- Once a task is marked 'DONE' in Roam, (and if you have installed the [Todo Trigger extension](https://github.com/tombarys/roam-depot-nautilus/blob/main/README.md#1-install-todo-trigger-extension-for-better-experience)) it's **tagged with the completion time** in format dHH:MM (e.g. d14:30), which is visually interpreted as a faint grey section on the spiral. It can serve as a visual log of tasks that have been done.

While more extended preparation before a workday seems involved, it greatly aids conscious day planning and eventual task optimization. 

## Setup
### 1. First, make sure that __User code__ is enabled in your settings. 
This allows custom components in your graph. 

<img src="https://github.com/8bitgentleman/roam-depot-tidy-todos/raw/main/settings.png" width="300"></img>

Technically, the spiral component is a code inserted using a Roam template into a block on a Daily Page. 


### 2. Install the TODO Trigger extension for a better experience
I strongly suggest installing great [David Vargas](https://github.com/dvargas92495/roamjs-todo-trigger)'s **Todo Trigger extension** from Roam Depot before using Nautilus and setting it to add a timestamp when todo is done automatically. 

<img src="https://github.com/tombarys/roam-depot-nautilus/blob/31e8113651badce77da0eabac5d4a6e4fa657b60/todotrigger.png?raw=true" width="300"></img>

## Daily usage
### 1. Insert the component into your Daily Page
The easiest way to insert the component is through Roam's native template menu. Type `;;` and look for „Nautilus.“ Press Enter.
<img src="https://github.com/8bitgentleman/roam-depot-tidy-todos/raw/main/template.png" max-width="400"></img>

### 3. Put your tasks and events into the children’s block 
- Move a list of your todos there. 

## Example 








