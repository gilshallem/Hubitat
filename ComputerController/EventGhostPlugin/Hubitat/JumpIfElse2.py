# -*- coding: utf-8 -*-
#
# This file is part of EventGhost.
# Copyright © 2005-2016 EventGhost Project <http://www.eventghost.org/>
#
# EventGhost is free software: you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free
# Software Foundation, either version 2 of the License, or (at your option)
# any later version.
#
# EventGhost is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
# more details.
#
# You should have received a copy of the GNU General Public License along
# with EventGhost. If not, see <http://www.gnu.org/licenses/>.

import wx

# Local imports
import eg

class JumpIfElse2(eg.ActionBase):
    name = 'If...Then...Else'
    description = (
        "Jumps to another macro, if the specified condition is "
        "fulfilled (with the option to specify the destination "
        "also for the second case)."
        "Not specifically related to Hubitat but good to have :)"
    )
    iconFile = "icons/condition"
    class text:
        text1 = "If (python expression):"
        text2 = "Jump to:"
        text3 = "and return after execution"
        text4 = "Else jump to:"
        explain = "use p for the event payload (p[0],p[1]... for array of parameters)\nExample: float(p)>50.00  this will sent the command only if the event parameter value is greater then 50"
        mesg1 = "Select the macro..."
        mesg2 = (
            "Please select the macro that should be executed, if the "
            "condition is fulfilled."
        )
        mesg3 = (
            "Please select the macro that should be executed, if the "
            "condition is not fulfilled."
        )
        choices = [
            "last action was successful",
            "last action was unsuccessful",
            "always"
        ]
        labels = [
            'If successful, jump to "%s"',
            'If unsuccessful, jump to "%s"',
            'Jump to "%s"',
            ' and return',
            ' else jump to "%s"'
        ]

    def __call__(
        self,
        link = None,
        cond = "",
        gosub = False,
        link2 = None,
        gosub2 = False
    ):
        p = eg.event.payload
        if eval(cond):
            if gosub:
                eg.programReturnStack.append(eg.programCounter)
            nextItem = link.target
            nextIndex = nextItem.parent.GetChildIndex(nextItem)
            eg.indent += 1
            eg.programCounter = (nextItem, nextIndex)
        else:
            if gosub2:
                eg.programReturnStack.append(eg.programCounter)
            nextItem = link2.target
            nextIndex = nextItem.parent.GetChildIndex(nextItem)
            eg.indent += 1
            eg.programCounter = (nextItem, nextIndex)
        return eg.result

    def Configure(
        self,
        link = None,
        cond="",
        gosub = False,
        link2 = None,
        gosub2 = False
    ):
        text = self.text
        panel = eg.ConfigPanel()
        condCtrl = panel.TextCtrl(cond)
        linkCtrl = panel.MacroSelectButton(
            eg.text.General.choose,
            text.mesg1,
            text.mesg2,
            link
        )
        gosubCtrl = panel.CheckBox(gosub, text.text3)
        linkCtrl2 = panel.MacroSelectButton(
            eg.text.General.choose,
            text.mesg1,
            text.mesg3,
            link2
        )
        gosubCtrl2 = panel.CheckBox(gosub2, text.text3)
        labels = (
            panel.StaticText(text.text1),
            panel.StaticText(text.text2),
            panel.StaticText(text.text4)
            
        )
        eg.EqualizeWidths(labels)

        # def onKind(evt = None):
        #     enable = kindCtrl.GetSelection() < 2
        #     labels[2].Enable(enable)
        #     linkCtrl2.Enable(enable)
        #     gosubCtrl2.Enable(enable)
        #     if not enable:
        #         linkCtrl2.textBox.textCtrl.ChangeValue("")
        #         linkCtrl2.treeLink = eg.TreeLink(
        #             eg.Utils.GetTopLevelWindow(panel).treeItem
        #         )
        #         linkCtrl2.macro = None
        #         gosubCtrl2.SetValue(False)
        #     if evt:
        #         evt.Skip()
        # kindCtrl.Bind(wx.EVT_CHOICE, onKind)
        # onKind()

        sizer = wx.FlexGridSizer(8, 2, 3, 5)
        sizer.AddGrowableCol(1, 1)
        sizer.Add(labels[0], 0, wx.ALIGN_CENTER_VERTICAL)
        sizer.Add(condCtrl, 1, wx.EXPAND)
        sizer.Add((0, 0))
        sizer.Add(panel.StaticText(text.explain))
        sizer.Add((0, 15))
        sizer.Add((0, 15))
        sizer.Add(labels[1], 0, wx.ALIGN_CENTER_VERTICAL)
        sizer.Add(linkCtrl, 1, wx.EXPAND)
        sizer.Add((0, 0))
        sizer.Add(gosubCtrl)
        sizer.Add((0, 15))
        sizer.Add((0, 15))
        sizer.Add(labels[2], 0, wx.ALIGN_CENTER_VERTICAL)
        sizer.Add(linkCtrl2, 1, wx.EXPAND)
        sizer.Add((0, 0))
        sizer.Add(gosubCtrl2)
        panel.sizer.Add(sizer, 1, wx.EXPAND, wx.ALIGN_CENTER_VERTICAL)

        while panel.Affirmed():
            lnk = linkCtrl.GetValue()
            lnk2 = linkCtrl2.GetValue()
            panel.SetResult(
                None if repr(lnk) == "XmlIdLink(-1)" else lnk,
                condCtrl.GetValue(),
                gosubCtrl.GetValue(),
                None if repr(lnk2) == "XmlIdLink(-1)" else lnk2,
                gosubCtrl2.GetValue()
            )

    def GetLabel(
        self,
        link = None,
        kind = 0,
        gosub = False,
        link2 = None,
        gosub2 = False
    ):
        labels = self.text.labels
        target = link.target if link is not None else None
        target2 = link2.target if link2 is not None else None
        res = (
            labels[kind] % (target.name if target is not None else "None")
        ) if link is not None else ""
        res += labels[3] if link is not None and gosub else ""
        res += (
            labels[4] % (target2.name if target2 is not None else "None")
        ) if link2 is not None else ""
        res += labels[3] if link2 is not None and gosub2 else ""
        return res if res else self.name
