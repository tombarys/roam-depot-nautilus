Nautilus is a Roam Research extension designed for effective, stress-free daily task planning. It shifts tasks and calendar events into a watch face-like spiral visual system within the Roam interface, acknowledging strongly that tasks vary in duration. 

Traditional time-boxing solution is be stressful due to rigid schedules; if one fails to keep up, the agenda quickly derails which causes demotivation. Nautilus is more flexible, dynamically repositioning unfinished tasks within your day's remaining open time slots. This reduce stress, improves prioritization and estimation skills, and clarifies feasible tasks at any given moment.

<img src="https://github.com/tombarys/roam-depot-nautilus/raw/main/nautilus-example.png" width="300"></img>

_Events (fixed time) are yellow, tasks are blue, done tasks are faint gray_


## General instructions

- Tasks/events are managed as usual **text lists** with simple markdown which Nautilus dynamically visualizes as watch-like visual timeline, based on real-time data.
- A notable feature is the automatic task **"push-forward," relocating uncompleted tasks** within open time slots in your day.
- Users write tasks in a very simple format:
  - **events** are rows containing a time range in 24h format (e.g., "12:30-13 Meeting with JK", "9-10 Breakfast")
  - **tasks** are rows the and are movable during a day (e.g., "{{[[TODO]]}} this important task (27min)").
- Tasks **duration defaults to a 15-minute time allocation**, but this can be adjusted generally above in settings or **manually and individually for each task** with simple notation "Mm" or "Mmin", where M is the length of task in minutes (e.g. "Call Jack 10m", "Daily workout 45min").
- The **order of tasks** in Nautilus spiral **reflects exactly the order of tasks in the list.** 
- Tasks can be only **forced to follow after a particular event** – by placing them after the event in the task list; for example, setting a run post-lunch. This does not change the order of tasks.
- Once a task is marked 'DONE' in Roam, it's **tagged with the completion time** in format dHH:MM (e.g. d14:30), which is visually interpreted as a faint grey section on the spiral. It can serve as a visual log of done tasks.
- While longer preparation phase seem involved, it greatly aids in conscious day planning and eventual task optimization. 

## Usage
First make sure that __User code__ is enabled in your settings. This allows custom components in your graph.
<img src="https://github.com/8bitgentleman/roam-depot-tidy-todos/raw/main/settings.png" width="300"></img>


### 1. Install TODO Trigger extension for better experience
I suggest to install David Vargas's TODO Trigger extension before using Nautilus and setting it to automatically add a timestamp when todo is done. 

<img src="https://github.com/tombarys/roam-depot-nautilus/raw/main/todotrigger.png" width="300"></img>

### 2. Insert the component into your Daily Page
Easiest way to insert the component is though Roam's native template menu. Simply type `;;` and look for __EXTENSION NAME HERE__
<img src="https://github.com/8bitgentleman/roam-depot-tidy-todos/raw/main/template.png" max-width="400"></img>



### 3. Put your tasks and events into the children block 
- Just move a list of your todos there. 

## Example 







